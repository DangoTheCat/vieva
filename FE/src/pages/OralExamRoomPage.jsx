import React from 'react';
import { CompletedStage } from '../components/exam/stages/CompletedStage';
import { FinalizingStage } from '../components/exam/stages/FinalizingStage';
import { InterruptedStage } from '../components/exam/stages/InterruptedStage';
import { NoAttemptView } from '../components/exam/stages/NoAttemptView';
import { ReconnectingStage } from '../components/exam/stages/ReconnectingStage';
import { useExamSession } from '../hooks/useExamSession';
import { InProgressStage } from '../components/exam/stages/InProgressStage';

export function OralExamRoomPage({
  onNavigate,
  showToast
}) {
  const {
    examStage,
    attemptId,
    questions,
    totalQuestions,
    activeQuestionIndex,
    secondsLeft,
    isSubmittingTurn,
    answerSecondsLeft,
    sttUnavailable,
    reconnectGraceLeft,
    isReconnecting,
    interruptedReason,
    finalizingStep,
    showSupportModal,
    setShowSupportModal,
    copiedAttempt,
    answersByQuestion,
    manualText,
    setManualText,
    examResult,
    isRecording,
    transcript,
    partialTranscript,
    isWsConnected,
    isAiSpeaking,
    playTTS,
    stopTTS,
    activeExam,
    currentQuestion,
    handleReconnect,
    handleToggleSpeak,
    handleSubmitTurn,
    handleFinishExam,
    copyToClipboard,
    studentName,
    studentCode,
  } = useExamSession({ onNavigate, showToast });

  // ==========================================
  // RENDER STATE: NO ATTEMPT
  // ==========================================
  if (!attemptId) {
    return <NoAttemptView onNavigate={onNavigate} />;
  }

  // ==========================================
  // RENDER STATE 1: READY (Auto-starting exam session)
  // ==========================================
  if (examStage === 'READY') {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center p-6 text-center space-y-4 animate-in fade-in duration-200">
        <div className="w-14 h-14 rounded-2xl bg-pink-100 text-pink-700 flex items-center justify-center shadow-xs mx-auto">
          <span className="w-6 h-6 border-2 border-pink-600 border-t-transparent rounded-full animate-spin"></span>
        </div>
        <div className="space-y-1">
          <h2 className="font-heading font-black text-xl text-ink">Đang Khởi Tạo Phòng Thi Vấn Đáp AI...</h2>
          <p className="text-xs text-slate-500">Thiết bị đã được hiệu chuẩn. Giám thị AI đang chuẩn bị câu hỏi cho bạn.</p>
        </div>
      </div>
    );
  }

  // ==========================================
  // RENDER STATE 3: RECONNECTING (Yellow Warning Modal/Banner)
  // ==========================================
  if (examStage === 'RECONNECTING') {
    return <ReconnectingStage handleReconnect={handleReconnect} isReconnecting={isReconnecting} onNavigate={onNavigate} reconnectGraceLeft={reconnectGraceLeft} />;
  }

  // ==========================================
  // RENDER STATE 4: FINALIZING (Full-screen overlay/banner)
  // ==========================================
  if (examStage === 'FINALIZING') {
    return <FinalizingStage finalizingStep={finalizingStep} transcript={transcript} />;
  }

  // ==========================================
  // RENDER STATE 6: INTERRUPTED (Red Neo-brutalist Card)
  // ==========================================
  if (examStage === 'INTERRUPTED') {
    return <InterruptedStage attemptId={attemptId} copiedAttempt={copiedAttempt} copyToClipboard={copyToClipboard} interruptedReason={interruptedReason} onNavigate={onNavigate} setShowSupportModal={setShowSupportModal} showSupportModal={showSupportModal} studentCode={studentCode} />;
  }

  // ==========================================
  // RENDER STATE 5: COMPLETED (Rubric Report & Certificate)
  // ==========================================
  if (examStage === 'COMPLETED') {
    return <CompletedStage activeExam={activeExam} attemptId={attemptId} examResult={examResult} onNavigate={onNavigate} questions={questions} studentCode={studentCode} studentName={studentName} />;
  }

  // ==========================================
  // RENDER STATE 2: IN_PROGRESS (Active Exam Room)
  // ==========================================
  return (
    <InProgressStage
      questions={questions}
      totalQuestions={totalQuestions}
      activeQuestionIndex={activeQuestionIndex}
      secondsLeft={secondsLeft}
      isSubmittingTurn={isSubmittingTurn}
      answerSecondsLeft={answerSecondsLeft}
      sttUnavailable={sttUnavailable}
      answersByQuestion={answersByQuestion}
      manualText={manualText}
      setManualText={setManualText}
      isRecording={isRecording}
      transcript={transcript}
      partialTranscript={partialTranscript}
      isWsConnected={isWsConnected}
      isAiSpeaking={isAiSpeaking}
      playTTS={playTTS}
      stopTTS={stopTTS}
      activeExam={activeExam}
      currentQuestion={currentQuestion}
      handleToggleSpeak={handleToggleSpeak}
      handleSubmitTurn={handleSubmitTurn}
      handleFinishExam={handleFinishExam}
      studentName={studentName}
      studentCode={studentCode}
    />
  );
}
