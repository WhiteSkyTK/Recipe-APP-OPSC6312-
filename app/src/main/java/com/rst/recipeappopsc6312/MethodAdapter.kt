package com.rst.recipeappopsc6312

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

class MethodAdapter(private val methodSteps: List<MethodStep>) :
    RecyclerView.Adapter<MethodAdapter.ViewHolder>() {

    inner class ViewHolder(val checkBox: MaterialCheckBox) : RecyclerView.ViewHolder(checkBox)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val checkBox = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_method_step, parent, false) as MaterialCheckBox
        return ViewHolder(checkBox)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = methodSteps[position]
        holder.checkBox.text = "${position + 1}. ${step.step}"
        holder.checkBox.isChecked = step.isCompleted
        setStrikeThrough(holder.checkBox, step.isCompleted)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            step.isCompleted = isChecked
            setStrikeThrough(holder.checkBox, isChecked)
            // In a real app, you might save this state locally
        }
    }

    private fun setStrikeThrough(checkBox: MaterialCheckBox, isChecked: Boolean) {
        if (isChecked) {
            checkBox.paintFlags = checkBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            checkBox.paintFlags = checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount(): Int = methodSteps.size
}