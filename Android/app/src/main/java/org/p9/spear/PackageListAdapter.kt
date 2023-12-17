package org.p9.spear

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView

class PackageListAdapter(
    initChecklist: List<String>,
    private val active: Boolean,
    onChecklistUpdate: (String, Boolean) -> Unit)
        : RecyclerView.Adapter<PackageListAdapter.PackageListViewHolder>() {

    private val packageList = mutableListOf<org.p9.spear.entity.Package>()
    private val checklistSet = initChecklist.toMutableSet()
    private val onChecklistUpdate = onChecklistUpdate

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageListViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.package_item, parent, false)
        return PackageListViewHolder(itemView, active) {
            name, checked ->
                if (checked) {
                    checklistSet.add(name)
                } else {
                    checklistSet.remove(name)
                }
                onChecklistUpdate(name, checked)
        }
    }

    override fun onBindViewHolder(holder: PackageListViewHolder, position: Int) {
        val pkg = packageList[position]
        holder.icon.setImageBitmap(pkg.icon.toBitmap())
        holder.name.text = pkg.name

        holder.enableListener = false
        holder.switch.isChecked = checklistSet.contains(pkg.name)
        holder.enableListener = true
    }

    override fun getItemCount(): Int = packageList.size

    fun setPackageList(newList: List<org.p9.spear.entity.Package>) {
        packageList.clear()
        packageList.addAll(newList)
        notifyDataSetChanged()
    }

    class PackageListViewHolder(
        itemView: View,
        active: Boolean,
        onChecklistUpdate: (String, Boolean) -> Unit)
            : RecyclerView.ViewHolder(itemView) {

        var enableListener: Boolean = false
        val icon: ImageView = itemView.findViewById(R.id.package_icon)
        val name: TextView = itemView.findViewById(R.id.package_name)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val switch: Switch = itemView.findViewById(R.id.package_switch)
        init {
            if (active) {
                switch.setOnCheckedChangeListener { _, isChecked ->
                    if (enableListener) {
                        onChecklistUpdate(name.text.toString(), isChecked)
                    }
                }
            } else {
                switch.isEnabled = false
            }
        }
    }
}