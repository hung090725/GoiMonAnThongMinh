package hung.edu.mealmindai.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * B12 - Helper quản lý nhận diện giọng nói tiếng Việt chuyên nghiệp.
 */
public class VoiceInputHelper {

    private static final String TAG = "VoiceInputHelper";
    private static final String LANGUAGE_VI = "vi-VN";
    private static final long LISTENING_TIMEOUT_MS = 12000L;

    private final Context context;
    private final VoiceInputCallback callback;
    private final Handler mainHandler;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean resultDelivered = false;

    private final Runnable timeoutRunnable;

    public interface VoiceInputCallback {
        void onReady();
        void onListening();
        void onResult(String text);
        void onError(String message);
        void onEnd();
    }

    public VoiceInputHelper(Context context, VoiceInputCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.timeoutRunnable = () -> {
            if (!isListening) {
                return;
            }
            Log.d(TAG, "Listening timeout");
            stopListening();
            safeCallback(() -> callback.onError("Không nghe thấy giọng nói. Hãy bấm micro và nói rõ nguyên liệu."));
            safeCallback(callback::onEnd);
        };
    }

    public static boolean isAvailable(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    public void startListening() {
        if (!isAvailable(context)) {
            safeCallback(() -> callback.onError("Thiết bị không hỗ trợ nhận diện giọng nói."));
            return;
        }

        mainHandler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
                
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                speechRecognizer.setRecognitionListener(createRecognitionListener());
                resultDelivered = false;
                
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_VI);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LANGUAGE_VI);
                intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L);
                intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói nguyên liệu bạn đang có");
                
                Log.d(TAG, "SpeechRecognizer.startListening() started");
                isListening = true;
                speechRecognizer.startListening(intent);
                mainHandler.removeCallbacks(timeoutRunnable);
                mainHandler.postDelayed(timeoutRunnable, LISTENING_TIMEOUT_MS);
            } catch (Exception e) {
                isListening = false;
                Log.e(TAG, "Lỗi khởi tạo: " + e.getMessage());
                safeCallback(() -> callback.onError("Lỗi khởi tạo: " + e.getMessage()));
                safeCallback(callback::onEnd);
            }
        });
    }

    private RecognitionListener createRecognitionListener() {
        return new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "onReadyForSpeech");
                safeCallback(callback::onReady);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech");
                safeCallback(callback::onListening);
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech");
            }

            @Override
            public void onError(int error) {
                mainHandler.removeCallbacks(timeoutRunnable);
                isListening = false;
                String message = getErrorMessage(error);
                Log.e(TAG, "onError: " + error + " - " + message);
                safeCallback(() -> callback.onError(message));
                safeCallback(callback::onEnd);
            }

            @Override
            public void onResults(Bundle results) {
                mainHandler.removeCallbacks(timeoutRunnable);
                isListening = false;
                resultDelivered = true;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    if (text != null && !text.trim().isEmpty()) {
                        Log.d(TAG, "final result: " + text);
                        safeCallback(() -> callback.onResult(text));
                    } else {
                        safeCallback(() -> callback.onError("Không nhận diện được giọng nói."));
                    }
                } else {
                    safeCallback(() -> callback.onError("Không nhận diện được giọng nói."));
                }
                safeCallback(callback::onEnd);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    Log.d(TAG, "partial: " + text);
                    if (text != null && !text.trim().isEmpty() && !resultDelivered) {
                        mainHandler.removeCallbacks(timeoutRunnable);
                        mainHandler.postDelayed(timeoutRunnable, LISTENING_TIMEOUT_MS);
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        };
    }

    public void stopListening() {
        isListening = false;
        mainHandler.removeCallbacks(timeoutRunnable);
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }
            safeCallback(callback::onEnd);
        });
    }

    public void destroy() {
        isListening = false;
        mainHandler.removeCallbacks(timeoutRunnable);
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
        });
    }

    public boolean isListening() {
        return isListening;
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Lỗi micro hoặc ghi âm.";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Lỗi phía thiết bị.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Chưa cấp quyền micro.";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Lỗi mạng khi nhận diện giọng nói.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Hết thời gian kết nối mạng.";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Không nhận diện được giọng nói. Hãy nói chậm và rõ hơn.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Bộ nhận diện đang bận, hãy thử lại.";
            case SpeechRecognizer.ERROR_SERVER:
                return "Lỗi máy chủ nhận diện.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Không nghe thấy giọng nói. Hãy thử lại.";
            default:
                return "Lỗi nhận diện giọng nói (" + errorCode + ")";
        }
    }

    private void safeCallback(Runnable action) {
        if (callback != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) action.run();
            else mainHandler.post(action);
        }
    }
}
