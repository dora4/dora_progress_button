package dora.widget

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.*

class MainActivity : AppCompatActivity() {

    var currentProgress: Float = 0f
    private var timer = Timer()
    var timerTask: TimerTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressButton = findViewById<ProgressButton>(R.id.progressButton)
        val progressButtonWithBorder = findViewById<ProgressButton>(R.id.progressButtonWithBorder)
        progressButton.mockProgress()
        progressButtonWithBorder.setOnClickListener {
            if (progressButtonWithBorder.isNew()) {
                if (timerTask != null) {
                    timer.cancel()
                    timer = Timer()
                }
                timerTask = object : TimerTask() {
                    override fun run() {
                        if (currentProgress < 100f) {
                            currentProgress += 0.9f
                        } else {
                            return
                        }
                        runOnUiThread {
                            if (currentProgress > 100f) {
                                currentProgress = 100f
                            }
                            progressButtonWithBorder.setProgressText("下载中 ", currentProgress)
                        }
                    }
                }
                timer.schedule(timerTask, 0, 100)
            } else {
                progressButtonWithBorder.pause()
                if (timer != null) {
                    timer.cancel()
                }
            }
        }
        progressButtonWithBorder.setOnProgressChangeListener(object : ProgressButton.OnProgressChangeListener {
            override fun onPause() {
            }

            override fun onPendingProgress(progress: Float) {
            }

            override fun onFinish() {
                timerTask?.cancel()
                timerTask = null
                currentProgress = 0f
            }
        })
    }
}