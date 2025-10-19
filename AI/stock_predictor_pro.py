import os
os.environ['TF_ENABLE_ONEDNN_OPTS'] = '0'  # Suppress oneDNN warning
import yfinance as yf
import numpy as np
import pandas as pd
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, Layer
from tensorflow.keras.callbacks import EarlyStopping, ModelCheckpoint
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.mixed_precision import set_global_policy
from xgboost import XGBRegressor
import ta
import pickle
import datetime
from sklearn.model_selection import train_test_split, GridSearchCV
import requests
import json
import keras_tuner as kt
from nltk.sentiment.vader import SentimentIntensityAnalyzer
import nltk
import logging
import joblib
import shap
import time
import tensorflow as tf
import warnings

# Suppress warnings
warnings.filterwarnings("ignore", category=FutureWarning)  # Suppress yfinance warning
tf.get_logger().setLevel('ERROR')  # Suppress TensorFlow warnings

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Download VADER lexicon for sentiment analysis
nltk.download('vader_lexicon')

# Enable mixed precision training for VRAM optimization
set_global_policy('mixed_float16')

# Custom Attention Layer
class CustomAttention(Layer):
    def __init__(self):
        super(CustomAttention, self).__init__()

    def build(self, input_shape):
        self.W = self.add_weight(name='attention_weight',
                                 shape=(input_shape[-1], 1),
                                 initializer='random_normal',
                                 trainable=True)
        self.b = self.add_weight(name='attention_bias',
                                 shape=(1,),
                                 initializer='zeros',
                                 trainable=True)
        super(CustomAttention, self).build(input_shape)

    def call(self, inputs):
        e = tf.matmul(inputs, self.W) + self.b
        e = tf.tanh(e)
        alpha = tf.nn.softmax(e, axis=1)
        context = tf.reduce_sum(inputs * alpha, axis=1)
        return context

# Function to fetch news sentiment (lightweight with VADER)
def fetch_sentiment(ticker, api_key, start_date, end_date):
    cache_file = f"sentiment_{ticker}.pkl"
    if os.path.exists(cache_file):
        logging.info(f"Loading cached sentiment for {ticker}")
        with open(cache_file, 'rb') as f:
            return pickle.load(f)
    
    # Skip sentiment for indices
    if ticker.startswith('^'):
        logging.info(f"Skipping sentiment for index {ticker}")
        sentiment_series = pd.Series(0, index=pd.date_range(start=start_date, end=end_date))
        with open(cache_file, 'wb') as f:
            pickle.dump(sentiment_series, f)
        return sentiment_series
    
    url = f"https://www.alphavantage.co/query?function=NEWS_SENTIMENT&tickers={ticker}&apikey={api_key}"
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = json.loads(response.text)
        if 'feed' not in data:
            logging.warning(f"No sentiment data for {ticker}")
            sentiment_series = pd.Series(0, index=pd.date_range(start=start_date, end=end_date))
        else:
            sia = SentimentIntensityAnalyzer()
            sentiment_scores = []
            dates = []
            for article in data['feed'][:50]:  # Limit to 50 articles
                date = pd.to_datetime(article['time_published'][:8])
                if start_date <= date <= end_date:
                    score = sia.polarity_scores(article['summary'])['compound']
                    sentiment_scores.append(score)
                    dates.append(date)
            sentiment_df = pd.DataFrame({'Sentiment': sentiment_scores}, index=dates)
            sentiment_series = sentiment_df.groupby(sentiment_df.index).mean().reindex(
                pd.date_range(start=start_date, end=end_date), method='ffill').fillna(0)['Sentiment']
        # Cache sentiment data
        with open(cache_file, 'wb') as f:
            pickle.dump(sentiment_series, f)
        time.sleep(12)  # Delay to respect Alpha Vantage rate limit (5 calls/min)
        return sentiment_series
    except Exception as e:
        logging.error(f"Sentiment fetch failed for {ticker}: {e}")
        return pd.Series(0, index=pd.date_range(start=start_date, end=end_date))

# Function to fetch macroeconomic data (VIX, interest rates)
def fetch_macro_data(start_date, end_date, api_key):
    cache_file = "macro_data.pkl"
    if os.path.exists(cache_file):
        logging.info("Loading cached macro data")
        with open(cache_file, 'rb') as f:
            return pickle.load(f)
    
    try:
        # VIX using yfinance as primary source
        vix_data = None
        try:
            logging.info("Fetching VIX data from yfinance")
            vix_stock = yf.download("^VIX", start=start_date, end=end_date, interval='1d', auto_adjust=False, timeout=30)
            if not vix_stock.empty:
                vix_data = vix_stock['Close'].iloc[:, 0]
        except Exception as e:
            logging.warning(f"yfinance VIX fetch failed: {e}")
        
        if vix_data is None:
            logging.info("Falling back to Alpha Vantage for VIX")
            url_vix = f"https://www.alphavantage.co/query?function=VIX&interval=daily&apikey={api_key}"
            response_vix = requests.get(url_vix)
            response_vix.raise_for_status()
            data_vix = json.loads(response_vix.text)
            if 'Time Series (Daily)' not in data_vix:
                logging.error(f"VIX API response missing 'Time Series (Daily)': {data_vix}")
                raise KeyError("VIX API response missing expected data")
            vix_data = pd.DataFrame(data_vix['Time Series (Daily)']).T['4. close'].astype(float)
            vix_data.index = pd.to_datetime(vix_data.index)
            time.sleep(12)  # Delay for rate limit
        
        # Interest rates (10-year Treasury)
        ir_data = None
        try:
            logging.info("Fetching Treasury yield data from Alpha Vantage")
            url_ir = f"https://www.alphavantage.co/query?function=TREASURY_YIELD&interval=daily&maturity=10year&apikey={api_key}"
            response_ir = requests.get(url_ir)
            response_ir.raise_for_status()
            data_ir = json.loads(response_ir.text)
            if 'data' not in data_ir:
                logging.error(f"Treasury API response missing 'data': {data_ir}")
                raise KeyError("Treasury API response missing expected data")
            # Convert to DataFrame, handle invalid values
            ir_df = pd.DataFrame(data_ir['data'])
            ir_df['date'] = pd.to_datetime(ir_df['date'])
            ir_df['value'] = pd.to_numeric(ir_df['value'], errors='coerce')
            ir_data = ir_df.set_index('date')['value'].sort_index()
            time.sleep(12)  # Delay for rate limit
        except Exception as e:
            logging.warning(f"Treasury yield fetch failed: {e}")
            ir_data = pd.Series(0, index=pd.date_range(start=start_date, end=end_date))
        
        macro_df = pd.DataFrame({'VIX': vix_data, 'Interest_Rate': ir_data}).reindex(
            pd.date_range(start=start_date, end=end_date), method='ffill').fillna(0)
        # Cache macro data
        with open(cache_file, 'wb') as f:
            pickle.dump(macro_df, f)
        return macro_df
    except Exception as e:
        logging.error(f"Macro data fetch failed: {e}")
        return pd.DataFrame({'VIX': 0, 'Interest_Rate': 0}, index=pd.date_range(start=start_date, end=end_date))

# Function to fetch and preprocess stock data
def fetch_stock_data(ticker, start_date, end_date, api_key):
    try:
        cache_file = f"stock_data_{ticker}.pkl"
        if os.path.exists(cache_file):
            logging.info(f"Loading cached stock data for {ticker}")
            with open(cache_file, 'rb') as f:
                return pickle.load(f)
        
        logging.info(f"Fetching stock data for {ticker}")
        # Retry logic for yfinance
        for attempt in range(3):
            try:
                stock_data = yf.download(ticker, start=start_date, end=end_date, interval='1d', auto_adjust=False, timeout=30)
                break
            except Exception as e:
                logging.warning(f"Attempt {attempt + 1} failed for {ticker}: {e}")
                if attempt < 2:
                    time.sleep(5)  # Wait before retry
                else:
                    raise Exception(f"Failed to fetch data for {ticker} after 3 attempts: {e}")
        
        if stock_data.empty:
            logging.error(f"No data returned for {ticker}")
            return None
        
        # Debug: Log shape and type of stock_data
        logging.info(f"stock_data['Close'] shape: {stock_data['Close'].shape}, type: {type(stock_data['Close'])}")
        logging.info(f"stock_data['Volume'] shape: {stock_data['Volume'].shape}, type: {type(stock_data['Volume'])}")
        logging.info(f"stock_data['High'] shape: {stock_data['High'].shape}, type: {type(stock_data['High'])}")
        logging.info(f"stock_data['Low'] shape: {stock_data['Low'].shape}, type: {type(stock_data['Low'])}")
        
        # Create new DataFrame with the same index
        df = pd.DataFrame(index=stock_data.index)
        # Assign columns as Series using iloc[:, 0]
        df['Close'] = stock_data['Close'].iloc[:, 0]
        df['Volume'] = stock_data['Volume'].iloc[:, 0]
        df['High'] = stock_data['High'].iloc[:, 0]
        df['Low'] = stock_data['Low'].iloc[:, 0]
        
        # Verify data is 1D
        for col in ['Close', 'Volume', 'High', 'Low']:
            if not isinstance(df[col], pd.Series) or df[col].ndim != 1:
                logging.error(f"Column {col} is not 1D: shape {df[col].shape}, type {type(df[col])}")
                return None
        
        # Technical indicators using df columns (Series)
        df['RSI'] = ta.momentum.RSIIndicator(df['Close'], window=14).rsi()
        df['MACD'] = ta.trend.MACD(df['Close']).macd()
        df['EMA_20'] = ta.trend.EMAIndicator(df['Close'], window=20).ema_indicator()
        df['SMA_50'] = ta.trend.SMAIndicator(df['Close'], window=50).sma_indicator()
        df['BB_upper'] = ta.volatility.BollingerBands(df['Close']).bollinger_hband()
        df['BB_lower'] = ta.volatility.BollingerBands(df['Close']).bollinger_lband()
        df['ATR'] = ta.volatility.AverageTrueRange(df['High'], df['Low'], df['Close']).average_true_range()
        df['OBV'] = ta.volume.OnBalanceVolumeIndicator(df['Close'], df['Volume']).on_balance_volume()
        df['Stochastic'] = ta.momentum.StochasticOscillator(df['High'], df['Low'], df['Close']).stoch()
        
        # Macro data
        macro_data = fetch_macro_data(start_date, end_date, api_key)
        df['VIX'] = macro_data['VIX'].reindex(df.index, method='ffill').fillna(0)
        df['Interest_Rate'] = macro_data['Interest_Rate'].reindex(df.index, method='ffill').fillna(0)
        
        # Sentiment
        df['Sentiment'] = fetch_sentiment(ticker, api_key, start_date, end_date).reindex(df.index, method='ffill').fillna(0)
        
        # Drop rows with NaN values
        df = df.dropna()
        logging.info(f"Successfully fetched and processed data for {ticker}: {df.shape}")
        
        # Cache stock data
        with open(cache_file, 'wb') as f:
            pickle.dump(df, f)
        
        return df
    except Exception as e:
        logging.error(f"Stock data fetch failed for {ticker}: {e}")
        return None

# Function to prepare data for LSTM
def prepare_lstm_data(df, look_back=60):
    features = ['Close', 'Volume', 'RSI', 'MACD', 'EMA_20', 'SMA_50', 'BB_upper', 'BB_lower', 'ATR', 'OBV', 'Stochastic', 'VIX', 'Interest_Rate', 'Sentiment']
    X, y = [], []
    for i in range(look_back, len(df)):
        X.append(df[features].iloc[i-look_back:i].values)
        y.append(df['Close'].iloc[i])
    X, y = np.array(X), np.array(y)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, shuffle=False)
    scaler = MinMaxScaler(feature_range=(0, 1))
    X_train_reshaped = X_train.reshape(-1, X_train.shape[-1])
    X_test_reshaped = X_test.reshape(-1, X_test.shape[-1])
    scaler.fit(X_train_reshaped)
    X_train = scaler.transform(X_train_reshaped).reshape(X_train.shape)
    X_test = scaler.transform(X_test_reshaped).reshape(X_test.shape)
    return X_train, X_test, y_train, y_test, scaler, features

# Function to prepare data for XGBoost
def prepare_xgb_data(df, look_back=60):
    features = ['Close', 'Volume', 'RSI', 'MACD', 'EMA_20', 'SMA_50', 'BB_upper', 'BB_lower', 'ATR', 'OBV', 'Stochastic', 'VIX', 'Interest_Rate', 'Sentiment']
    X, y = [], []
    for i in range(look_back, len(df)):
        row = df[features].iloc[i-look_back:i].mean()
        X.append(row)
        y.append(df['Close'].iloc[i])
    X, y = np.array(X), np.array(y)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, shuffle=False)
    scaler = MinMaxScaler(feature_range=(0, 1))
    scaler.fit(X_train)
    X_train = scaler.transform(X_train)
    X_test = scaler.transform(X_test)
    return X_train, X_test, y_train, y_test, scaler, features

# Function to build LSTM model with attention
def build_lstm_model(hp):
    model = Sequential()
    model.add(LSTM(
        units=hp.Int('units_1', min_value=100, max_value=400, step=50),
        return_sequences=True,
        input_shape=(60, 14)))  # 14 features
    model.add(Dropout(hp.Float('dropout_1', 0.2, 0.5, step=0.1)))
    model.add(LSTM(
        units=hp.Int('units_2', min_value=50, max_value=200, step=50),
        return_sequences=True))
    model.add(Dropout(hp.Float('dropout_2', 0.2, 0.5, step=0.1)))
    model.add(CustomAttention())
    model.add(Dropout(hp.Float('dropout_3', 0.2, 0.5, step=0.1)))
    model.add(Dense(units=1, dtype='float32'))
    
    model.compile(
        optimizer=Adam(
            learning_rate=hp.Float('learning_rate', 1e-4, 1e-2, sampling='log'),
            clipnorm=1.0),  # Gradient clipping
        loss='mean_squared_error')
    return model

# Function to train LSTM with hyperparameter tuning
def build_and_train_lstm(ticker, api_key, save_path="lstm_model.h5", scaler_path="lstm_scaler.pkl"):
    try:
        end_date = datetime.datetime.now()
        start_date = end_date - datetime.timedelta(days=5*365)  # 5 years for testing
        df = fetch_stock_data(ticker, start_date, end_date, api_key)
        if df is None or df.empty:
            logging.error("No data fetched for training")
            return None, None, None, None
        
        # Prepare data
        X_train, X_test, y_train, y_test, scaler, features = prepare_lstm_data(df)
        
        # Hyperparameter tuning
        tuner = kt.Hyperband(
            build_lstm_model,
            objective='val_loss',
            max_epochs=100,
            factor=3,
            directory='tuner_dir',
            project_name=f'stock_lstm_{ticker}')
        
        early_stopping = EarlyStopping(monitor='val_loss', patience=20, restore_best_weights=True)
        checkpoint = ModelCheckpoint(save_path, monitor='val_loss', save_best_only=True)
        
        tuner.search(X_train, y_train, epochs=100, validation_data=(X_test, y_test), 
                     callbacks=[early_stopping, checkpoint], batch_size=16)
        
        # Get best model
        model = tuner.get_best_models(num_models=1)[0]
        
        # Evaluate
        train_pred = model.predict(X_train, batch_size=16)
        test_pred = model.predict(X_test, batch_size=16)
        train_mae = mean_absolute_error(y_train, train_pred)
        test_mae = mean_absolute_error(y_test, test_pred)
        test_rmse = np.sqrt(mean_squared_error(y_test, test_pred))
        directional_acc = np.mean(np.sign(test_pred[1:] - test_pred[:-1]) == np.sign(y_test[1:] - y_test[:-1]))
        logging.info(f"LSTM - Train MAE: {train_mae:.4f}, Test MAE: {test_mae:.4f}, Test RMSE: {test_rmse:.4f}, Directional Accuracy: {directional_acc:.4f}")
        
        # Save scaler
        with open(scaler_path, 'wb') as f:
            pickle.dump(scaler, f)
        
        return model, scaler, df, features
    except Exception as e:
        logging.error(f"LSTM training failed: {e}")
        return None, None, None, None

# Function to train XGBoost with grid search and feature selection
def build_and_train_xgb(ticker, api_key, save_path="xgb_model.pkl", scaler_path="xgb_scaler.pkl"):
    try:
        end_date = datetime.datetime.now()
        start_date = end_date - datetime.timedelta(days=5*365)  # 5 years for testing
        df = fetch_stock_data(ticker, start_date, end_date, api_key)
        if df is None or df.empty:
            logging.error("No data fetched for training")
            return None, None, None, None
        
        # Prepare data
        X_train, X_test, y_train, y_test, scaler, features = prepare_xgb_data(df)
        
        # Grid search
        param_grid = {
            'n_estimators': [500, 1000],
            'learning_rate': [0.01, 0.05],
            'max_depth': [4, 6]
        }
        model = XGBRegressor()
        grid_search = GridSearchCV(model, param_grid, cv=3, scoring='neg_mean_squared_error', verbose=1, n_jobs=-1)
        grid_search.fit(X_train, y_train)
        
        # Best model
        model = grid_search.best_estimator_
        
        # Feature importance with SHAP
        explainer = shap.TreeExplainer(model)
        shap_values = explainer.shap_values(X_train)
        feature_importance = pd.DataFrame({'Feature': features, 'Importance': np.abs(shap_values).mean(0)})
        logging.info(f"Feature Importance:\n{feature_importance.sort_values(by='Importance', ascending=False)}")
        
        # Evaluate
        train_pred = model.predict(X_train)
        test_pred = model.predict(X_test)
        train_mae = mean_absolute_error(y_train, train_pred)
        test_mae = mean_absolute_error(y_test, test_pred)
        test_rmse = np.sqrt(mean_squared_error(y_test, test_pred))
        directional_acc = np.mean(np.sign(test_pred[1:] - test_pred[:-1]) == np.sign(y_test[1:] - y_test[:-1]))
        logging.info(f"XGBoost - Train MAE: {train_mae:.4f}, Test MAE: {test_mae:.4f}, Test RMSE: {test_rmse:.4f}, Directional Accuracy: {directional_acc:.4f}")
        
        # Save model and scaler
        with open(save_path, 'wb') as f:
            pickle.dump(model, f)
        with open(scaler_path, 'wb') as f:
            pickle.dump(scaler, f)
        
        return model, scaler, df, features
    except Exception as e:
        logging.error(f"XGBoost training failed: {e}")
        return None, None, None, None

# Function to backtest predictions with transaction costs
def backtest_predictions(predictions, actuals, threshold=0.015, transaction_cost=0.001):
    returns = []
    for i in range(1, len(predictions)):
        if predictions[i] > actuals[i-1] * (1 + threshold):
            returns.append((actuals[i] - actuals[i-1]) / actuals[i-1] - transaction_cost)  # Buy
        elif predictions[i] < actuals[i-1] * (1 - threshold):
            returns.append((actuals[i-1] - actuals[i]) / actuals[i-1] - transaction_cost)  # Sell
    sharpe_ratio = np.mean(returns) / np.std(returns) * np.sqrt(252) if returns else 0
    max_drawdown = np.min(np.cumsum(returns)) if returns else 0
    return np.mean(returns) * 100 if returns else 0, sharpe_ratio, max_drawdown

# Function to optimize ensemble weights
def optimize_ensemble_weights(lstm_preds, xgb_preds, actuals):
    best_weight = 0.5
    best_rmse = float('inf')
    for w in np.arange(0.1, 0.9, 0.1):
        combined = w * lstm_preds + (1 - w) * xgb_preds
        rmse = np.sqrt(mean_squared_error(actuals, combined))
        if rmse < best_rmse:
            best_rmse = rmse
            best_weight = w
    return best_weight

# Function to predict and provide buy/sell advice
def predict_and_advise(ticker, api_key, lstm_model_path="lstm_model.h5", lstm_scaler_path="lstm_scaler.pkl", 
                      xgb_model_path="xgb_model.pkl", xgb_scaler_path="xgb_scaler.pkl"):
    try:
        from tensorflow.keras.models import load_model
        lstm_model = load_model(lstm_model_path, custom_objects={'CustomAttention': CustomAttention})
        with open(lstm_scaler_path, 'rb') as f:
            lstm_scaler = pickle.load(f)
        with open(xgb_model_path, 'rb') as f:
            xgb_model = pickle.load(f)
        with open(xgb_scaler_path, 'rb') as f:
            xgb_scaler = pickle.load(f)
        
        end_date = datetime.datetime.now()
        start_date = end_date - datetime.timedelta(days=90)
        df = fetch_stock_data(ticker, start_date, end_date, api_key)
        if df is None or df.empty:
            logging.error("No data fetched for prediction")
            return None
        
        # LSTM prediction
        features = ['Close', 'Volume', 'RSI', 'MACD', 'EMA_20', 'SMA_50', 'BB_upper', 'BB_lower', 'ATR', 'OBV', 'Stochastic', 'VIX', 'Interest_Rate', 'Sentiment']
        lstm_scaled_data = lstm_scaler.transform(df[features])
        lstm_sequence = lstm_scaled_data[-60:]
        lstm_sequence = np.expand_dims(lstm_sequence, axis=0)
        lstm_pred = lstm_model.predict(lstm_sequence, batch_size=16)
        lstm_pred = lstm_scaler.inverse_transform(
            np.concatenate([lstm_pred, np.zeros((1, len(features)-1))], axis=1))[:, 0]
        
        # XGBoost prediction
        xgb_data = xgb_scaler.transform(df[features][-60:].mean().values.reshape(1, -1))
        xgb_pred = xgb_model.predict(xgb_data)
        
        # Optimize ensemble weights on recent data
        recent_data = df['Close'].tail(30).values
        recent_lstm_preds, recent_xgb_preds = [], []
        for i in range(30, 0, -1):
            lstm_seq = lstm_scaled_data[-60-i:-i]
            if len(lstm_seq) == 60:
                lstm_seq = np.expand_dims(lstm_seq, axis=0)
                lp = lstm_model.predict(lstm_seq, batch_size=16)
                lp = lstm_scaler.inverse_transform(np.concatenate([lp, np.zeros((1, len(features)-1))], axis=1))[:, 0]
                xp = xgb_model.predict(xgb_scaler.transform(df[features][-60-i:-i].mean().values.reshape(1, -1)))
                recent_lstm_preds.append(lp[0])
                recent_xgb_preds.append(xp[0])
        lstm_weight = optimize_ensemble_weights(np.array(recent_lstm_preds), np.array(recent_xgb_preds), recent_data)
        
        # Ensemble prediction
        final_pred = lstm_weight * lstm_pred[0] + (1 - lstm_weight) * xgb_pred[0]
        
        # Backtest
        recent_preds = [lstm_weight * lp + (1 - lstm_weight) * xp for lp, xp in zip(recent_lstm_preds, recent_xgb_preds)]
        backtest_return, sharpe_ratio, max_drawdown = backtest_predictions(recent_preds, recent_data)
        
        current_price = df['Close'].iloc[-1]
        threshold = 0.015
        confidence = abs(final_pred - current_price) / current_price
        advice = "Buy" if final_pred > current_price * (1 + threshold) else \
                 "Sell" if final_pred < current_price * (1 - threshold) else "Hold"
        
        return {
            "ticker": ticker,
            "current_price": current_price,
            "predicted_price": float(final_pred),
            "advice": advice,
            "confidence": float(confidence),
            "backtest_return": float(backtest_return),
            "sharpe_ratio": float(sharpe_ratio),
            "max_drawdown": float(max_drawdown),
            "historical_data": df[['Close']].tail(30).to_dict(),
            "lstm_weight": float(lstm_weight)
        }
    except Exception as e:
        logging.error(f"Prediction failed: {e}")
        return None

# Function to train models for multiple tickers
def train_multi_ticker(tickers, api_key):
    results = {}
    for ticker in tickers:
        logging.info(f"Training for {ticker}")
        lstm_model, lstm_scaler, df, lstm_features = build_and_train_lstm(ticker, api_key, 
                                                                         save_path=f"lstm_model_{ticker}.h5", 
                                                                         scaler_path=f"lstm_scaler_{ticker}.pkl")
        xgb_model, xgb_scaler, df, xgb_features = build_and_train_xgb(ticker, api_key, 
                                                                     save_path=f"xgb_model_{ticker}.pkl", 
                                                                     scaler_path=f"xgb_scaler_{ticker}.pkl")
        if lstm_model and xgb_model:
            result = predict_and_advise(ticker, api_key, 
                                      lstm_model_path=f"lstm_model_{ticker}.h5", 
                                      lstm_scaler_path=f"lstm_scaler_{ticker}.pkl",
                                      xgb_model_path=f"xgb_model_{ticker}.pkl", 
                                      xgb_scaler_path=f"xgb_scaler_{ticker}.pkl")
            results[ticker] = result
    return results

if __name__ == "__main__":
    # Replace with your Alpha Vantage API key (store securely in production)
    API_KEY = "HW0915KN1KB7G3DZ"
    tickers = ["^GSPC", "AAPL", "MSFT"]  # S&P 500, Apple, Microsoft
    results = train_multi_ticker(tickers, API_KEY)
    for ticker, result in results.items():
        if result:
            logging.info(f"Results for {ticker}: {result}")
