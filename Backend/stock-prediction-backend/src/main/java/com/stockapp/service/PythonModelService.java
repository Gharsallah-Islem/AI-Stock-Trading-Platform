package com.stockapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.dto.PredictionDTO;
import com.stockapp.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PythonModelService {

    private final String pythonScriptPath = "C:/Users/islem/OneDrive/Bureau/project/AI/predict_live.py";
    private final String pythonExecutable = "C:/Users/islem/OneDrive/Bureau/project/.venv/Scripts/python.exe"; // Or
                                                                                                               // specify
                                                                                                               // a full
                                                                                                               // path
                                                                                                               // to a
                                                                                                               // virtual
                                                                                                               // env
                                                                                                               // python
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PredictionDTO generatePrediction(String ticker, LocalDate targetDate, String modelType) {
        // The targetDate is now calculated inside the Python script, so we don't need
        // to pass it.
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    pythonScriptPath,
                    ticker,
                    modelType);

            // Set working directory to AI folder so models can be found
            pb.directory(new java.io.File("C:/Users/islem/OneDrive/Bureau/project/AI"));
            // Don't redirect error stream - we want to separate stdout (JSON) from stderr
            // (warnings)
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    // Only capture lines that look like JSON (start with { and end with })
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("{") && trimmedLine.endsWith("}")) {
                        output.append(trimmedLine);
                        break; // We expect only one JSON response, so stop after finding it
                    }
                }

                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ApiException("Python script failed with exit code: " + exitCode +
                        ", output: " + output + ", errors: " + errorOutput);
            }

            // Parse JSON output
            String jsonOutput = output.toString().trim();

            // Add debugging
            System.out.println("Raw JSON output captured: '" + jsonOutput + "'");
            System.out.println("Error output: '" + errorOutput.toString() + "'");

            if (jsonOutput.isEmpty()) {
                throw new ApiException("Empty output from Python script. Errors: " + errorOutput);
            }

            JsonNode jsonNode = objectMapper.readTree(jsonOutput);

            if (jsonNode.has("error")) {
                throw new ApiException("Python script error: " + jsonNode.get("error").asText());
            }

            // Create PredictionDTO from the result
            PredictionDTO dto = new PredictionDTO();
            dto.setSymbol(jsonNode.get("symbol").asText());
            dto.setPredictedPrice(jsonNode.get("predictedPrice").asDouble());
            dto.setPredictionDate(LocalDate.parse(jsonNode.get("predictionDate").asText().substring(0, 10)));
            dto.setTargetDate(LocalDate.parse(jsonNode.get("targetDate").asText().substring(0, 10)));
            dto.setConfidenceScore(jsonNode.get("confidenceScore").asDouble());
            dto.setAdvice(jsonNode.get("advice").asText());
            dto.setModelType(jsonNode.get("modelType").asText());

            return dto;

        } catch (IOException | InterruptedException e) {
            throw new ApiException("Error executing Python prediction script: " + e.getMessage());
        } catch (Exception e) {
            throw new ApiException("Error parsing Python script output: " + e.getMessage());
        }
    }

    /**
     * Check if Python and required packages are available
     */
    public boolean isPythonAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}