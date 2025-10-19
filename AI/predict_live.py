import os
os.environ['TF_ENABLE_ONEDNN_OPTS'] = '0'
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

import sys
import json
import datetime
import yfinance as yf
import numpy as np
import pandas as pd
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import load_model
from tensorflow.keras.layers import Layer
import tensorflow as tf
import pickle
import ta
import logging
import warnings

# --- Setup ---
warnings.filterwarnings("ignore", category=FutureWarning)
logging.basicConfig(filename='predict_live.log', level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Custom Attention Layer Definition (Required for loading the model) ---
class CustomAttention(Layer):
    def __init__(self, **kwargs):
        super(CustomAttention, self).__init__(**kwargs)

    def build(self, input_shape):
        self.W = self.add_weight(name='attention_weight', shape=(input_shape[-1], 1), initializer='random_normal', trainable=False)
        self.b = self.add_weight(name='attention_bias', shape=(1,), initializer='zeros', trainable=False)
        super(CustomAttention, self).build(input_shape)

    def call(self, inputs):
        e = tf.matmul(inputs, self.W) + self.b
        e = tf.tanh(e)
        alpha = tf.nn.softmax(e, axis=1)
        context = tf.reduce_sum(inputs * alpha, axis=1)
        return context

    def get_config(self):
        return super(CustomAttention, self).get_config()

# --- Data Fetching and Processing Functions ---
def fetch_live_stock_data(ticker, days=90):
    """Fetches the last 90 days of stock data and calculates technical indicators."""
    try:
        end_date = datetime.datetime.now()
        start_date = end_date - datetime.timedelta(days=days)
        
        stock_data = yf.download(ticker, start=start_date, end=end_date, interval='1d', auto_adjust=True, progress=False)
        
        if stock_data.empty:
            raise ValueError(f"No data returned from yfinance for {ticker}")

        # Calculate technical indicators
        df = stock_data.copy()
        df['RSI'] = ta.momentum.RSIIndicator(df['Close'].squeeze(), window=14).rsi()
        df['MACD'] = ta.trend.MACD(df['Close'].squeeze()).macd()
        df['EMA_20'] = ta.trend.EMAIndicator(df['Close'].squeeze(), window=20).ema_indicator()
        df['SMA_50'] = ta.trend.SMAIndicator(df['Close'].squeeze(), window=50).sma_indicator()
        df['BB_upper'] = ta.volatility.BollingerBands(df['Close'].squeeze()).bollinger_hband()
        df['BB_lower'] = ta.volatility.BollingerBands(df['Close'].squeeze()).bollinger_lband()
        df['ATR'] = ta.volatility.AverageTrueRange(df['High'].squeeze(), df['Low'].squeeze(), df['Close'].squeeze()).average_true_range()
        df['OBV'] = ta.volume.OnBalanceVolumeIndicator(df['Close'].squeeze(), df['Volume'].squeeze()).on_balance_volume()
        df['Stochastic'] = ta.momentum.StochasticOscillator(df['High'].squeeze(), df['Low'].squeeze(), df['Close'].squeeze()).stoch()
        
        # For simplicity in this live script, we'll use 0 for VIX, Interest Rate, and Sentiment
        df['VIX'] = 0
        df['Interest_Rate'] = 0
        df['Sentiment'] = 0
        
        df = df.dropna()
        return df
    except Exception as e:
        logging.error(f"Failed to fetch live stock data for {ticker}: {e}")
        raise

# --- Main Prediction Logic ---
def predict(ticker, model_type):
    """Loads models, fetches data, and makes a prediction."""
    try:
        # Define paths
        base_path = os.path.dirname(os.path.abspath(__file__))
        lstm_model_path = os.path.join(base_path, f"lstm_model_{ticker}.h5")
        xgb_model_path = os.path.join(base_path, f"xgb_model_{ticker}.pkl")
        lstm_scaler_path = os.path.join(base_path, f"lstm_scaler_{ticker}.pkl")
        xgb_scaler_path = os.path.join(base_path, f"xgb_scaler_{ticker}.pkl")

        # --- Load Models and Scalers ---
        lstm_model = load_model(lstm_model_path, custom_objects={'CustomAttention': CustomAttention})
        with open(lstm_scaler_path, 'rb') as f:
            lstm_scaler = pickle.load(f)
            
        with open(xgb_model_path, 'rb') as f:
            xgb_model = pickle.load(f)
        with open(xgb_scaler_path, 'rb') as f:
            xgb_scaler = pickle.load(f)

        # --- Fetch and Prepare Data ---
        df = fetch_live_stock_data(ticker)
        features = ['Close', 'Volume', 'RSI', 'MACD', 'EMA_20', 'SMA_50', 'BB_upper', 'BB_lower', 'ATR', 'OBV', 'Stochastic', 'VIX', 'Interest_Rate', 'Sentiment']
        
        # Ensure all feature columns exist
        for col in features:
            if col not in df.columns:
                raise ValueError(f"Feature column '{col}' not found in fetched data.")

        # --- Make Predictions ---
        # LSTM Prediction
        last_60_days_lstm = df[features].tail(60).values
        last_60_days_lstm_scaled = lstm_scaler.transform(last_60_days_lstm)
        X_test_lstm = np.array([last_60_days_lstm_scaled])
        lstm_pred_scaled = lstm_model.predict(X_test_lstm, verbose=0)
        
        # Create a dummy array for inverse transform
        # The scaler expects the same number of features as it was trained on
        dummy_array_lstm = np.zeros((1, len(features)))
        dummy_array_lstm[:, 0] = lstm_pred_scaled.flatten()
        lstm_pred = lstm_scaler.inverse_transform(dummy_array_lstm)[0, 0]

        # XGBoost Prediction
        # The original model expects a 1D array of feature averages.
        last_60_days_xgb = df[features].tail(60).mean().values
        # The scaler expects a 2D array, so we reshape for scaling
        last_60_days_xgb_scaled_2d = xgb_scaler.transform(last_60_days_xgb.reshape(1, -1))
        # The model itself expects the scaled data for prediction
        xgb_pred = xgb_model.predict(last_60_days_xgb_scaled_2d)[0]

        # --- Ensemble Prediction ---
        # Use a fixed weight for live prediction to ensure speed
        lstm_weight = 0.6
        ensemble_pred = (lstm_weight * lstm_pred) + ((1 - lstm_weight) * xgb_pred)

        # --- Select Final Prediction based on model_type ---
        if model_type.upper() == 'LSTM':
            final_pred = lstm_pred
        elif model_type.upper() == 'XGBOOST':
            final_pred = xgb_pred
        else: # Default to Ensemble
            final_pred = ensemble_pred
            model_type = 'Ensemble'

        # --- Generate Advice and Confidence ---
        current_price = float(df['Close'].iloc[-1])
        confidence = 1 - (abs(final_pred - current_price) / current_price) * 2
        confidence = max(0.5, min(0.98, confidence)) # Clamp confidence between 50% and 98%

        threshold = 0.01 # 1% change threshold
        if final_pred > current_price * (1 + threshold):
            advice = "Buy"
        elif final_pred < current_price * (1 - threshold):
            advice = "Sell"
        else:
            advice = "Hold"

        # --- Prepare JSON Output ---
        result = {
            "symbol": ticker,
            "predictedPrice": float(final_pred),
            "predictionDate": datetime.datetime.now().isoformat(),
            "targetDate": (datetime.datetime.now() + datetime.timedelta(days=1)).isoformat(),
            "confidenceScore": float(confidence),
            "advice": advice,
            "modelType": model_type
        }
        
        return result

    except FileNotFoundError as e:
        logging.error(f"Model file not found for {ticker}: {e}")
        raise
    except Exception as e:
        logging.error(f"Prediction failed for {ticker} with model {model_type}: {e}")
        raise

# --- Script Execution ---
if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(json.dumps({"error": "Usage: python predict_live.py <TICKER> <MODEL_TYPE>"}), file=sys.stderr)
        sys.exit(1)
    
    ticker_symbol = sys.argv[1]
    model_selection = sys.argv[2]
    
    try:
        prediction_result = predict(ticker_symbol, model_selection)
        print(json.dumps(prediction_result))
    except Exception as e:
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(1)
