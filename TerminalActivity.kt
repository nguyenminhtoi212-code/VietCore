package com.example.myempty.vietcore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * TerminalActivity: Trung tâm điều khiển của VietCore OS.
 * Đã đồng bộ Strings, xử lý bàn phím và bảng điều khiển phía trên.
 * Developer: Nguyen Minh Toi.
 */
class TerminalActivity : AppCompatActivity() {

    private lateinit var tvConsole: TextView
    private lateinit var etCommand: EditText
    private lateinit var scrollConsole: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        // Ánh xạ View từ XML
        tvConsole = findViewById(R.id.tv_terminal_output)
        etCommand = findViewById(R.id.et_terminal_input)
        scrollConsole = findViewById(R.id.scroll_terminal)

        // Xử lý nút hiện bàn phím (Cảm ứng thời gian thực)
        findViewById<ImageView>(R.id.btn_show_keyboard).setOnClickListener {
            etCommand.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // Sử dụng SHOW_FORCED để đảm bảo bàn phím luôn hiện khi chạm
            imm.showSoftInput(etCommand, InputMethodManager.SHOW_FORCED)
            logToConsole(getString(R.string.log_keyboard_requested))
        }

        // Xử lý nút thoát (Icon Power)
        findViewById<ImageView>(R.id.btn_terminal_exit).setOnClickListener {
            showExitMenu()
        }

        // Lắng nghe sự kiện gõ phím Enter/Done
        etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                executeCommand(etCommand.text.toString())
                true
            } else false
        }
    }

    /**
     * Ghi log ra màn hình console với dấu chấm phân tách và thời gian.
     */
    private fun logToConsole(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        // Thêm dấu chấm đầu dòng giúp hiển thị rõ các chương trình đang chạy
        tvConsole.append("\n. [$time] $message")
        
        // Tự động cuộn xuống dòng mới nhất
        scrollConsole.post { 
            scrollConsole.fullScroll(View.FOCUS_DOWN) 
        }
    }

    /**
     * Xử lý các lệnh được nhập từ người dùng.
     */
    private fun executeCommand(cmd: String) {
        val input = cmd.trim()
        if (input.isEmpty()) return

        // Hiển thị lại lệnh đã gõ kèm prompt (như terminal thực thụ)
        logToConsole("${getString(R.string.terminal_prompt)}$input")
        
        when (input.lowercase()) {
            "clear" -> {
                tvConsole.text = getString(R.string.log_session_reset)
                logToConsole(getString(R.string.log_awaiting))
            }
            "status" -> {
                logToConsole("VietCore OS: STABLE | Kernel: v1.0.2-2026")
            }
            "exit" -> showExitMenu()
            else -> {
                // Sử dụng chuỗi định dạng từ strings.xml để hiển thị lệnh đang chạy
                val execMsg = getString(R.string.log_exec_proc, input)
                logToConsole(execMsg)
            }
        }
        etCommand.text.clear()
    }

    /**
     * Hiển thị bảng chọn xác nhận thoát phiên làm việc.
     */
    private fun showExitMenu() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.terminal_exit_title))
            .setMessage(getString(R.string.terminal_exit_msg))
            .setPositiveButton(getString(R.string.terminal_btn_exit)) { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.terminal_btn_cancel), null)
            .show()
    }
}
