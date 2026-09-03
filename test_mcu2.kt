import com.google.android.material.color.utilities.MaterialDynamicColors
import java.lang.reflect.Method

fun main() {
    val mdcClass = MaterialDynamicColors::class.java
    for (method in mdcClass.methods) {
        if (method.name == "primary" || method.name == "primaryContainer") {
            println(method.name)
        }
    }
}
