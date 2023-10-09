package com.example.graphviewdemo.customview.data

enum class KamleonGraphViewMode(val identifier: String) {
    Daily("daily"),
    Weekly("weekly"),
    Monthly("monthly"),
    Yearly("yearly");

//    fun displayName() : String {
//        return when(this) {
//            Daily -> "Daily"
//            Weekly -> "Weekly"
//            Monthly -> "Monthly"
//            Yearly -> "Yearly"
//        }
//    }

    fun xLabelStrings(steps: Int) : ArrayList<KamleonGraphAxisLabelItem> {
        val aryRet = ArrayList<KamleonGraphAxisLabelItem>()
        if (this == Daily) {
            for (step in 0 until steps step 2 ) {
                aryRet.add(KamleonGraphAxisLabelItem(step.toDouble(), step.toString()))

            }
        } else if (this == Monthly) {
            for (step in 0 until steps step 2 ) {
                aryRet.add(KamleonGraphAxisLabelItem(step.toDouble(), (step + 1).toString()))

            }
        } else if (this == Weekly) {
            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            for (step in dayNames.indices) {
                aryRet.add(KamleonGraphAxisLabelItem(step.toDouble(), dayNames[step]))
            }
        } else if (this == Yearly) {
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            for (step in monthNames.indices) {
                aryRet.add(KamleonGraphAxisLabelItem(step.toDouble(), monthNames[step]))
            }
        }

        return aryRet
    }

    companion object {
        fun viewMode(fromIndex: Int) : KamleonGraphViewMode {
            return when(fromIndex) {
                1 -> Weekly
                2 -> Monthly
                3 -> Yearly
                else -> Daily
            }
        }
    }
}