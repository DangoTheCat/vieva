import React from 'react';
import { CompletedStage } from '../components/exam/stages/CompletedStage';
import { FinalizingStage } from '../components/exam/stages/FinalizingStage';
import { InterruptedStage } from '../components/exam/stages/InterruptedStage';
import { NoAttemptView } from '../components/exam/stages/NoAttemptView';
import { ReadyStage } from '../components/exam/stages/ReadyStage';
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
    preflightMicOk,
    setPreflightMicOk,
    preflightSpeakerOk,
    preflightTestingMic,
    preflightMicVolume,
    isRecording,
    transcript,
    partialTranscript,
    isWsConnected,
    isAiSpeaking,
    playTTS,
    stopTTS,
    activeExam,
    currentQuestion,
    startPreflightMicTest,
    stopPreflightMic,
    handleTestSpeaker,
    handleStartExam,
    handleReconnect,
    handleToggleSpeak,
    handleSubmitTurn,
    handleFinishExam,
    copyToClipboard,
    studentName,
    studentCode,
  } = useExamSession({ onNavigate, showToast });

  // ==========================================
  // RENDER STATE 1: READY (Pre-flight check)
  // ==========================================
  if (!attemptId) {
    return <NoAttemptView onNavigate={onNavigate} />;
  }

  if (examStage === 'READY') {
    return <ReadyStage handleStartExam={handleStartExam} handleTestSpeaker={handleTestSpeaker} isAiSpeaking={isAiSpeaking} preflightMicOk={preflightMicOk} preflightMicVolume={preflightMicVolume} preflightSpeakerOk={preflightSpeakerOk} preflightTestingMic={preflightTestingMic} setPreflightMicOk={setPreflightMicOk} startPreflightMicTest={startPreflightMicTest} stopPreflightMic={stopPreflightMic} studentCode={studentCode} studentName={studentName} />;
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
