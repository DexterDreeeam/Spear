package org.p9.spear

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PackageListAdapter(initChecklist: String, onChecklistUpdate: (String, Boolean) -> Unit) : RecyclerView.Adapter<PackageListAdapter.PackageListViewHolder>() {

    private val packageList = mutableListOf<org.p9.spear.entity.Package>()
    private val initChecklistSet = mutableSetOf<String>()
    private val onChecklistUpdate = onChecklistUpdate

    init {
        for (packageName in initChecklist.split(",")) {
            initChecklistSet.add(packageName)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.package_item, parent, false)
        return PackageListViewHolder(itemView, onChecklistUpdate)
    }

    override fun onBindViewHolder(holder: PackageListViewHolder, position: Int) {
        val pkg = packageList[position]
        holder.icon.setImageDrawable(pkg.icon)
        holder.name.text = pkg.name

        holder.enableListener = false
        holder.switch.isChecked = initChecklistSet.contains(pkg.name)
        holder.enableListener = true
    }

    override fun getItemCount(): Int = packageList.size

    fun setPackageList(newList: List<org.p9.spear.entity.Package>) {
        packageList.clear()
        packageList.addAll(newList)
        notifyDataSetChanged()
    }

    class PackageListViewHolder(itemView: View, onChecklistUpdate: (String, Boolean) -> Unit) : RecyclerView.ViewHolder(itemView) {
        var enableListener: Boolean = false
        val icon: ImageView = itemView.findViewById(R.id.package_icon)
        val name: TextView = itemView.findViewById(R.id.package_name)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val switch: Switch = itemView.findViewById(R.id.package_switch)
        init {
            switch.setOnCheckedChangeListener { _, isChecked ->
                if (enableListener) {
                    onChecklistUpdate(name.text.toString(), isChecked)
                }
            }
        }
    }
}