import React from 'react';
import { isFollowUp, turnBadge } from '../../../utils/turnTypes';
import { AudioVisualizerCanvas } from '../AudioVisualizerCanvas';
import { formatTime } from '../examRoomModel';
import { Mic, MicOff, Radio, Send, Sparkles, Square, Volume2 } from 'lucide-react';

/** Exam room — IN_PROGRESS stage (the live interview). Extracted from OralExamRoomPage (plan item 2.12). */
export function InProgressStage({ questions, totalQuestions, activeQuestionIndex, secondsLeft, isSubmittingTurn, answerSecondsLeft, sttUnavailable, answersByQuestion, manualText, setManualText, isRecording, transcript, partialTranscript, isWsConnected, isAiSpeaking, playTTS, stopTTS, activeExam, currentQuestion, handleToggleSpeak, handleSubmitTurn, handleFinishExam, studentName, studentCode }) {
  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12 animate-in fade-in duration-200 text-ink">
      
      {/* 1. Header: Exam Title, Question counter, Timer, Reconnect Test Trigger */}
      <div className="bg-white border border-pink-200 rounded-3xl p-5 shadow-md flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-slate-100 border border-pink-200 text-slate-700 flex items-center justify-center text-xl shadow-sm shrink-0">
            🎙️
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-heading font-black text-base sm:text-lg text-ink">
                Phòng Thi Vấn Đáp Trực Tuyến (Oral Exam Room)
              </h1>
              <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border flex items-center gap-1 ${
                isWsConnected 
                  ? 'bg-emerald-100 text-emerald-800 border-emerald-300' 
                  : 'bg-amber-100 text-amber-800 border-amber-300'
              }`}>
                <Radio className={`w-3 h-3 ${isWsConnected ? 'text-emerald-600 animate-pulse' : 'text-amber-600'}`} />
                {isWsConnected ? 'Nhận dạng giọng nói: sẵn sàng' : 'Nhận dạng giọng nói: chờ kết nối'}
              </span>
            </div>
            <p className="text-xs text-green-800 mt-0.5">
              Thí sinh: <strong>{studentName} ({studentCode})</strong> • Ca thi: <strong>{activeExam?.title || '—'}</strong>
            </p>
          </div>
        </div>

        {/* Question Counter & Countdown Timer */}
        <div className="flex items-center gap-3 w-full sm:w-auto justify-end flex-wrap">
          <div className="text-right font-mono bg-white border border-pink-200 px-3.5 py-1.5 rounded-xl shadow-sm">
            <span className="text-xs font-bold text-green-700 uppercase tracking-wider block">Tiến độ câu hỏi</span>
            <p className="text-xs sm:text-sm font-extrabold text-ink">
              Lượt {activeQuestionIndex + 1}{totalQuestions ? ` • ${totalQuestions} câu chính` : ''}
            </p>
          </div>

          <div aria-live="polite" aria-label="Thời gian ca thi còn lại" className={`text-right font-mono px-3.5 py-1.5 rounded-xl border transition-all duration-300 ${
            secondsLeft <= 30 && secondsLeft > 0
              ? 'bg-rose-100 border-rose-300 text-rose-900 animate-pulse shadow-sm ring-2 ring-rose-200' 
              : 'bg-pink-50 border-pink-200 text-pink-900 shadow-2xs'
          }`}>
            <span className={`text-xs font-bold uppercase tracking-wider block ${secondsLeft <= 30 && secondsLeft > 0 ? 'text-rose-700 font-extrabold' : 'text-pink-700'}`}>
              {secondsLeft <= 30 && secondsLeft > 0 ? '⚡ Sắp Hết Giờ!' : 'Thời Gian Ca Thi'}
            </span>
            <p className={`text-xs sm:text-sm font-black ${secondsLeft <= 30 && secondsLeft > 0 ? 'text-rose-800' : 'text-pink-900'}`}>
              {formatTime(secondsLeft)}
            </p>
          </div>

          {/* Per-answer timer: starts after the question has been read aloud */}
          <div
            aria-live="polite"
            className={`text-right font-mono px-3.5 py-1.5 rounded-xl border transition-all duration-300 ${
              answerSecondsLeft != null && answerSecondsLeft <= 15
                ? 'bg-rose-100 border-rose-300 text-rose-900 ring-2 ring-rose-200'
                : 'bg-white border-pink-200 text-ink shadow-2xs'
            }`}
          >
            <span className="text-xs font-bold uppercase tracking-wider block text-green-700">
              Thời gian trả lời câu này
            </span>
            <p className="text-xs sm:text-sm font-black">
              {answerSecondsLeft == null ? (isAiSpeaking ? 'Đang đọc câu hỏi' : '--:--') : formatTime(answerSecondsLeft)}
            </p>
          </div>

          <button
            type="button"
            onClick={() => handleFinishExam('USER_EARLY_SUBMIT')}
            className="px-5 py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] cursor-pointer"
          >
            Nộp Bài Thi ↗
          </button>
        </div>
      </div>

      {/* Main 2-Column Viva Exam Studio */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left Column (7 cols): Center Stage AI Examiner & Question Card */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Center Stage: AI Examiner Avatar with pulsing soundwave animation */}
          <div className="bg-white border border-pink-200 rounded-3xl p-6 shadow-md flex flex-col sm:flex-row items-center gap-6 relative overflow-hidden">
            {/* AI Avatar */}
            <div className="relative shrink-0">
              <div className={`w-28 h-28 rounded-3xl bg-pink-100 border border-pink-200 flex items-center justify-center text-5xl shadow-sm relative transition-transform ${
                isAiSpeaking ? 'scale-105 ring-4 ring-pink-400' : ''
              }`}>
                👩‍🏫
              </div>
              {/* Pulsing indicator */}
              <div className={`absolute -bottom-2 -right-2 px-2 py-0.5 rounded-full text-xs font-bold border-2 ${
                isAiSpeaking 
                  ? 'bg-pink-600 text-white border-blue-700 animate-pulse' 
                  : 'bg-white text-slate-700 border-pink-200 shadow-2xs'
              }`}>
                {isAiSpeaking ? 'Đang đọc...' : 'Chờ phản hồi'}
              </div>
            </div>

            {/* AI Examiner Info & Controls */}
            <div className="space-y-3 flex-1 text-center sm:text-left">
              <div>
                <span className="text-xs font-bold text-green-700 uppercase tracking-wider block">
                  Giám Thị Khảo Thí Trí Tuệ Nhân Tạo
                </span>
                <h3 className="font-heading font-black text-xl text-ink">
                  Dr. Sophia (AIVES AI Examiner)
                </h3>
              </div>

              {/* High-performance Canvas soundwave when TTS is speaking */}
              <div className="h-10 flex items-center justify-center sm:justify-start">
                {isAiSpeaking ? (
                  <div className="flex items-center gap-2 px-3 py-1.5 bg-pink-100 border border-pink-200 rounded-xl shadow-2xs">
                    <AudioVisualizerCanvas isActive={true} volume={0.85} barCount={14} width={130} height={22} primaryColor="#CE9893" accentColor="#819266" />
                    <span className="text-xs font-bold text-pink-950 ml-1">Đang đọc câu hỏi...</span>
                  </div>
                ) : (
                  <span className="text-xs text-slate-600 italic">
                    Nhấn "Nghe Lại Câu Hỏi" nếu bạn muốn giám thị đọc lại đề bài.
                  </span>
                )}
              </div>

              <div className="flex items-center justify-center sm:justify-start gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => playTTS(currentQuestion.content)}
                  className="px-3.5 py-1.5 rounded-xl bg-white hover:bg-green-50 text-ink font-bold text-xs border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center gap-1.5"
                >
                  <Volume2 className="w-3.5 h-3.5 text-pink-600" />
                  <span>Nghe Lại Câu Hỏi</span>
                </button>
                {isAiSpeaking && (
                  <button
                    type="button"
                    onClick={stopTTS}
                    className="px-3 py-1.5 rounded-xl bg-rose-100 hover:bg-rose-200 text-rose-800 font-bold text-xs border border-pink-200 shadow-sm cursor-pointer flex items-center gap-1"
                  >
                    <Square className="w-3 h-3 text-rose-600" />
                    <span>Dừng Đọc</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Question Card: Question content, badge "Câu hỏi chính" hoặc "Câu hỏi xoáy" */}
          <div className="bg-white border border-pink-200 rounded-3xl p-6 shadow-md space-y-4">
            <div className="flex items-start justify-between gap-3 pb-3 border-b border-pink-200">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border-2 ${
                    isFollowUp(currentQuestion.type)
                      ? 'bg-rose-50 text-rose-800 border-rose-200'
                      : 'bg-green-100 text-green-900 border-green-400'
                  }`}>
                    {currentQuestion.badge || turnBadge(currentQuestion.type)}
                  </span>
                  <span className="text-xs font-mono font-bold text-green-700">
                    Mã lượt: #{currentQuestion.turnId || currentQuestion.id}
                  </span>
                </div>
                <h2 className="font-heading font-black text-lg sm:text-xl text-ink leading-snug pt-1">
                  {currentQuestion.title}
                </h2>
              </div>
            </div>

            <div className="bg-white border border-pink-200 rounded-2xl p-4 shadow-sm">
              <p className="text-sm sm:text-base text-ink leading-relaxed font-medium">
                {currentQuestion.content}
              </p>
            </div>

            {currentQuestion.hint && (
              <div className="p-3 bg-slate-50 border border-pink-200 rounded-xl text-xs text-slate-700 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-pink-600 shrink-0" />
                <span><strong>Gợi ý sư phạm:</strong> {currentQuestion.hint}</span>
              </div>
            )}
          </div>

        </div>

        {/* Right Column (5 cols): Answer Section & Action Controls */}
        <div className="lg:col-span-5 space-y-6">
          
          <div className="bg-white border border-pink-200 rounded-3xl p-6 shadow-md space-y-4">
            
            <div className="flex items-center justify-between pb-3 border-b border-pink-200">
              <div className="flex items-center gap-2">
                <span className={`w-3 h-3 rounded-full border border-pink-200 ${
                  isRecording ? 'bg-rose-500 animate-ping' : 'bg-emerald-500'
                }`} />
                <h3 className="font-heading font-black text-sm text-ink uppercase tracking-wider">
                  Khu Vực Trả Lời Vấn Đáp
                </h3>
              </div>
              <span className="text-xs font-mono text-green-700 font-bold">
                {isRecording ? 'Đang Thu Âm...' : 'Micro Sẵn Sàng'}
              </span>
            </div>

            {/* Realtime 60fps Canvas Visualizer when student speaks */}
            {isRecording && (
              <div className="p-3 bg-paper rounded-2xl border border-pink-200 flex items-center justify-between shadow-2xs animate-in fade-in">
                <div className="flex items-center gap-2.5">
                  <AudioVisualizerCanvas isActive={true} volume={0.9} barCount={18} width={150} height={24} primaryColor="#CE9893" accentColor="#819266" />
                  <span className="text-xs text-pink-950 font-bold">Đang nghe câu trả lời của bạn...</span>
                </div>
              </div>
            )}

            {/* Realtime transcript live typing effect */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-green-800 uppercase tracking-wider block">
                Nội dung câu trả lời (Bóc băng giọng nói &amp; Soạn thảo):
              </label>
              
              <div className="relative">
                {/* Oral exam: answers are spoken. Typing is only a fallback while speech recognition is unavailable. */}
                <textarea
                  rows={6}
                  value={sttUnavailable ? manualText : [transcript, partialTranscript].filter(Boolean).join(' ')}
                  onChange={(e) => { if (sttUnavailable) setManualText(e.target.value); }}
                  readOnly={!sttUnavailable}
                  aria-label="Nội dung câu trả lời"
                  placeholder={sttUnavailable
                    ? 'Nhận dạng giọng nói đang gián đoạn: bạn vẫn ghi âm câu trả lời, có thể gõ thêm nội dung chính tại đây.'
                    : "Nhấn 'Bắt đầu nói' bên dưới và trả lời bằng giọng nói; nội dung sẽ hiện tại đây."}
                  className={`w-full p-4 rounded-2xl border border-pink-200 focus:border-pink-500 focus:ring-0 text-ink text-xs sm:text-sm leading-relaxed placeholder:text-slate-400 font-medium shadow-sm ${sttUnavailable ? 'bg-white' : 'bg-slate-50 cursor-default'}`}
                />
              </div>

              {/* Live transcript preview if recording */}
              {(transcript || partialTranscript) && (
                <div aria-live="polite" className="p-3 bg-slate-50 border border-pink-200 rounded-xl text-xs text-slate-800 font-medium animate-in fade-in">
                  <span className="font-bold text-pink-800 block text-xs uppercase tracking-wider mb-0.5">
                    Đang nhận dạng:
                  </span>
                  <span>{transcript} </span>
                  <span className="text-pink-600 font-bold underline">{partialTranscript}</span>
                </div>
              )}
            </div>

            {/* Action Controls: "Bắt đầu nói", "Dừng & Nộp câu trả lời" */}
            <div className="pt-2 space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={handleToggleSpeak}
                  className={`py-3 px-4 rounded-2xl font-black text-xs border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 ${
                    isRecording
                      ? 'bg-rose-600 hover:bg-rose-700 text-white animate-pulse'
                      : 'bg-white hover:bg-green-50 text-ink'
                  }`}
                >
                  {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4 text-pink-600" />}
                  <span>{isRecording ? 'Dừng Ghi Âm' : 'Bắt Đầu Nói'}</span>
                </button>

                <button
                  type="button"
                  onClick={handleSubmitTurn}
                  disabled={isSubmittingTurn}
                  className="py-3 px-4 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-black text-xs border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <Send className="w-4 h-4" />
                  <span>{isSubmittingTurn ? 'Đang Nộp...' : 'Dừng & Nộp Câu Trả Lời'}</span>
                </button>
              </div>

              <div className="flex items-center justify-between text-xs text-green-800 pt-1">
                <span>Tiêu chí: {currentQuestion.rubricCriteria}</span>
              </div>
            </div>

          </div>

          {/* Question Stepper / Overview */}
          <div className="bg-white border border-pink-200 rounded-3xl p-5 shadow-md space-y-3">
            <h4 className="font-heading font-black text-xs uppercase tracking-wider text-green-800">
              Danh Sách Lượt Vấn Đáp
            </h4>
            <div className="space-y-2">
              {questions.map((q, idx) => {
                const isCurrent = activeQuestionIndex === idx;
                const isAnswered = !!answersByQuestion[idx];
                return (
                  <div
                    key={q.id || idx}
                    className={`p-2.5 rounded-xl border-2 transition flex items-center justify-between text-xs ${
                      isCurrent
                        ? 'bg-pink-600 text-white border-slate-900 font-semibold shadow-xs'
                        : isAnswered
                        ? 'bg-emerald-50 border-emerald-400 text-emerald-900'
                        : 'bg-white border-green-300 text-green-900'
                    }`}
                  >
                    <div className="flex items-center gap-2 truncate">
                      <span className={`w-5 h-5 rounded-lg flex items-center justify-center text-xs font-bold ${
                        isCurrent ? 'bg-white text-ink font-bold' : isAnswered ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-700'
                      }`}>
                        {idx + 1}
                      </span>
                      <span className="truncate">{q.title || `Câu ${idx + 1}`}</span>
                    </div>
                    {isAnswered && (
                      <span className="text-xs font-bold text-emerald-700 shrink-0">Đã nộp ✓</span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

        </div>

      </div>

    </div>
  );
}
