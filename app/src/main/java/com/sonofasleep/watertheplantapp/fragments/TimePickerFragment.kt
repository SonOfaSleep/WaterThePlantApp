package com.sonofasleep.watertheplantapp.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.sonofasleep.watertheplantapp.R
import com.sonofasleep.watertheplantapp.viewmodels.AddNewPlantViewModel
import java.util.*

class TimePickerFragment(private val viewModel: AddNewPlantViewModel)
    : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private lateinit var calendar: Calendar

    // For setting current time
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        calendar = Calendar.getInstance()

        // Current time
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(
            activity,
            R.style.TimePickerTheme,
            this,
            hour,
            minutes,
            DateFormat.is24HourFormat(activity)
        )
    }

    override fun onTimeSet(p0: TimePicker?, hour: Int, minutes: Int) {
        val timeButton = activity?.findViewById<Button>(R.id.timeButton)
        timeButton?.text = timeFormat(hour, minutes)
        viewModel.setPlantTime(hour, minutes)
    }
}

fun timeFormat(hour: Int, min: Int): String = String.format("%02d:%02d", hour, min)