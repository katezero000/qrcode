package com.example.qrscanner

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.qrscanner.databinding.ActivityScanHistoryBinding
import java.text.DateFormat
import java.util.Date

class ScanHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanHistoryBinding
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = HistoryAdapter(this, ScanHistoryStore.getEntries(this))
        binding.listHistory.adapter = adapter
        binding.listHistory.setOnItemClickListener { _, _, position, _ ->
            val entry = adapter.getItem(position) ?: return@setOnItemClickListener
            val intent = Intent(this, ScanResultActivity::class.java)
            intent.putExtra(ScanResultActivity.EXTRA_RESULT, entry.content)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun refreshHistory() {
        adapter.clear()
        adapter.addAll(ScanHistoryStore.getEntries(this))
        adapter.notifyDataSetChanged()
    }

    private class HistoryAdapter(
        activity: AppCompatActivity,
        entries: List<HistoryEntry>
    ) : ArrayAdapter<HistoryEntry>(activity, 0, entries) {

        private val inflater = LayoutInflater.from(activity)
        private val dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(
                R.layout.item_scan_history,
                parent,
                false
            )
            val contentView = view.findViewById<TextView>(R.id.text_content)
            val timeView = view.findViewById<TextView>(R.id.text_time)
            val entry = getItem(position)
            contentView.text = entry?.content.orEmpty()
            val timestamp = entry?.timestamp ?: 0L
            timeView.text = if (timestamp > 0L) {
                dateFormat.format(Date(timestamp))
            } else {
                ""
            }
            return view
        }
    }
}
