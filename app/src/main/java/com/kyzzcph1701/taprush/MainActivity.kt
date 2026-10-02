package com.kyzzcph1701.taprush

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import java.util.Locale
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private lateinit var tapArea: FrameLayout
    private lateinit var countText: TextView
    private lateinit var cpsText: TextView
    private lateinit var comboText: TextView
    private lateinit var timeText: TextView
    private lateinit var bestText: TextView
    private lateinit var tapLabel: TextView
    private lateinit var challengeProgress: ProgressBar
    private lateinit var challengePercent: TextView

    private var count=0; private var combo=0; private var best=0
    private var startedAt=0L; private var lastTap=0L; private var deadline=0L
    private var running=false; private var modeSeconds=0; private var sound=false
    private val taps=ArrayDeque<Long>()
    private val handler=Handler(Looper.getMainLooper())
    private var tone: ToneGenerator?=null
    private val prefs by lazy { getSharedPreferences("taprush", Context.MODE_PRIVATE) }

    private val ticker=object:Runnable{
        override fun run(){ updateUi(); if(running) handler.postDelayed(this,50) }
    }

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        tapArea=findViewById(R.id.tapArea); countText=findViewById(R.id.countText)
        cpsText=findViewById(R.id.cpsText); comboText=findViewById(R.id.comboText)
        timeText=findViewById(R.id.timeText); bestText=findViewById(R.id.bestText)
        tapLabel=findViewById(R.id.tapLabel); challengeProgress=findViewById(R.id.challengeProgress)
        challengePercent=findViewById(R.id.challengePercent)
        best=prefs.getInt("best",0); bestText.text=best.toString()

        findViewById<RadioGroup>(R.id.modeGroup).setOnCheckedChangeListener{_,id->
            modeSeconds=when(id){R.id.mode10->10;R.id.mode30->30;else->0};resetGame()
        }
        tapArea.setOnTouchListener{_,e->if(e.action==MotionEvent.ACTION_DOWN) tap(e.x,e.y);true}
        findViewById<Button>(R.id.resetButton).setOnClickListener{resetGame()}
        findViewById<Button>(R.id.soundButton).setOnClickListener{
            sound=!sound; it as Button
            it.text=if(sound)"Sound: ON" else "Sound: OFF"; if(sound) beep()
        }
        updateUi()
    }

    private fun tap(x:Float,y:Float){
        val now=System.currentTimeMillis()
        if(modeSeconds>0&&!running&&count>0)return
        if(!running){running=true;startedAt=now;deadline=now+modeSeconds*1000L;handler.post(ticker)}
        combo=if(lastTap>0&&now-lastTap<650)combo+1 else 1
        lastTap=now;count++;taps.addLast(now)
        while(taps.isNotEmpty()&&now-taps.first()>1000)taps.removeFirst()
        if(count>best){best=count;bestText.text=best.toString();prefs.edit().putInt("best",best).apply()}
        countText.animate().scaleX(.92f).scaleY(.92f).setDuration(40).withEndAction{
            countText.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
        }.start()
        val plus=TextView(this).apply{text=if(combo>=5)"+1  x$combo" else "+1";textSize=17f;setTextColor(-1);setTypeface(typeface,1)}
        tapArea.addView(plus);plus.x=(x-30).coerceAtLeast(8f);plus.y=(y-20).coerceAtLeast(20f)
        plus.animate().translationYBy(-75f).alpha(0f).setDuration(600).withEndAction{tapArea.removeView(plus)}.start()
        if(sound)beep();updateUi()
    }

    private fun beep(){try{if(tone==null)tone=ToneGenerator(AudioManager.STREAM_MUSIC,55);tone?.startTone(ToneGenerator.TONE_PROP_BEEP,45)}catch(_:Exception){}}

    private fun updateUi(){
        val now=System.currentTimeMillis()
        while(taps.isNotEmpty()&&now-taps.first()>1000)taps.removeFirst()
        countText.text=count.toString();cpsText.text=String.format(Locale.US,"%.1f",taps.size.toFloat());comboText.text=combo.toString()
        val elapsed=if(startedAt==0L)0.0 else (now-startedAt)/1000.0
        timeText.text=String.format(Locale.US,"%.1fs",elapsed)
        if(modeSeconds>0&&running){
            val left=(deadline-now).coerceAtLeast(0L)
            tapLabel.text=String.format(Locale.US,"%.1f DETIK",left/1000.0)
            if(left<=0){running=false;statusDone();return}
        }
        challengeProgress.progress=min(100,count);challengePercent.text="${min(100,count)}%"
    }

    private fun statusDone(){handler.removeCallbacks(ticker);tapLabel.text="SELESAI — RESET UNTUK MAIN LAGI"}

    private fun resetGame(){
        running=false;handler.removeCallbacks(ticker);count=0;combo=0;startedAt=0;lastTap=0;deadline=0;taps.clear()
        for(i in tapArea.childCount-1 downTo 0){val v=tapArea.getChildAt(i);if(v is TextView&&v!==countText&&v!==tapLabel)tapArea.removeViewAt(i)}
        tapLabel.text=if(modeSeconds==0)"TAP DI MANA SAJA" else "$modeSeconds DETIK — TAP DI MANA SAJA";updateUi()
    }

    override fun onDestroy(){handler.removeCallbacksAndMessages(null);tone?.release();tone=null;super.onDestroy()}
}
