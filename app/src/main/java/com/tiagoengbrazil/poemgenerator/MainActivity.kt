package com.tiagoengbrazil.poemgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isGenerating = false // Flag para evitar múltiplas execuções

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val eTPrompt = findViewById<EditText>(R.id.inputEditText)
        val btnSubmit = findViewById<Button>(R.id.submitButton)
        val tvResult = findViewById<TextView>(R.id.responseTextView)
        val btnCopy = findViewById<Button>(R.id.copyButton)
        val textOrientation = findViewById<TextView>(R.id.textOrientation)

        val regex = Regex("^[a-zA-Z0-9À-ÿ\\s]*$")

        btnSubmit.setOnClickListener {
            val prompt = eTPrompt.text.toString()

            // Esconder o teclado
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(eTPrompt.windowToken, 0)

            // Validação do prompt
            if (prompt.isEmpty()) {
                Toast.makeText(
                    this, getString(R.string.string_1), Toast.LENGTH_SHORT
                ).show()
                btnCopy.visibility = View.GONE
                textOrientation.visibility = View.GONE
                return@setOnClickListener
            }

            if (!regex.matches(prompt)) {
                eTPrompt.text.clear()
                Toast.makeText(
                    this,
                    getString(R.string.string_2),
                    Toast.LENGTH_SHORT
                ).show()
                btnCopy.visibility = View.GONE
                textOrientation.visibility = View.GONE
                return@setOnClickListener
            }

            val modifiedPrompt =
                getString(R.string.write_poem1) + prompt + getString(R.string.string_2)

            generatePoem(modifiedPrompt, tvResult, btnCopy, textOrientation)
        }

        btnCopy.setOnClickListener {
            val generatedText = tvResult.text.toString()
            if (generatedText.isNotBlank()) {
                // Copia o texto para a área de transferência
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.string_3), generatedText)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(
                    this, getString(R.string.string_4), Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, getString(R.string.string_5), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generatePoem(
        prompt: String,
        tvResult: TextView,
        btnCopy: Button,
        textOrientation: TextView
    ) {
        if (isGenerating) return // Impede múltiplas execuções simultâneas

        isGenerating = true
        tvResult.text = ""
        startGeneratingAnimation(tvResult) // Inicia a animação

        val generativeModel = GenerativeModel(
            modelName = getString(R.string.model_name),
            apiKey = getString(R.string.key)
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(prompt)

                withContext(Dispatchers.Main) {
                    stopGeneratingAnimation() // Para a animação
                    isGenerating = false // Libera a flag

                    val generatedPoem = response.text ?: ""
                    tvResult.text = if (generatedPoem.isNotBlank()) {
                        btnCopy.visibility = View.VISIBLE
                        textOrientation.visibility = View.VISIBLE
                        generatedPoem
                    } else {
                        btnCopy.visibility = View.GONE
                        textOrientation.visibility = View.GONE
                        getString(R.string.string_6)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    stopGeneratingAnimation() // Para a animação
                    isGenerating = false // Libera a flag
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.string_7) + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    btnCopy.visibility = View.GONE
                    textOrientation.visibility = View.GONE
                }
            }
        }
    }

    private fun startGeneratingAnimation(textView: TextView) {
        textView.visibility = View.VISIBLE
        isGenerating = true
        val baseText = getString(R.string.string_8)
        var dotCount = 0

        handler.post(object : Runnable {
            override fun run() {
                if (isGenerating) {
                    textView.text = baseText + ".".repeat(dotCount)
                    dotCount = (dotCount + 1) % 4 // Alterna entre 0 e 3 pontos
                    handler.postDelayed(this, 500)
                }
            }
        })
    }

    private fun stopGeneratingAnimation() {
        isGenerating = false
        handler.removeCallbacksAndMessages(null)
    }
}
