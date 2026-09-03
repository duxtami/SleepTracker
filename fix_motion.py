import re

with open("app/src/main/java/com/sleeptracker/app/ui/navigation/FloatingNavBar.kt", "r") as f:
    content = f.read()

# Add imports for cubic-bezier if needed
content = content.replace("import androidx.compose.animation.core.tween", "import androidx.compose.animation.core.tween\nimport androidx.compose.animation.core.CubicBezierEasing")

# define easing
easing_def = """
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

private const val SHORT2 = 100
private const val SHORT3 = 150
private const val SHORT4 = 200
private const val MEDIUM1 = 250
private const val MEDIUM2 = 300
private const val LONG1 = 450
"""

# Insert easing_def after the @Composable imports or enum
content = content.replace("enum class NavTrailingAction", easing_def + "\nenum class NavTrailingAction")

# Replace spring(...) with tween(MEDIUM2, easing = EmphasizedEasing)
content = re.sub(r'spring\(dampingRatio = [^,]+,\s*stiffness = [^\)]+\)', 'tween(MEDIUM2, easing = EmphasizedEasing)', content)
content = re.sub(r'tween\(220\)', 'tween(SHORT4, easing = EmphasizedEasing)', content)
content = re.sub(r'tween\(180\)', 'tween(SHORT3, easing = EmphasizedDecelerateEasing)', content)
content = re.sub(r'tween\(120\)', 'tween(SHORT2, easing = EmphasizedAccelerateEasing)', content)
content = re.sub(r'tween\(150\)', 'tween(SHORT3, easing = EmphasizedAccelerateEasing)', content)

with open("app/src/main/java/com/sleeptracker/app/ui/navigation/FloatingNavBar.kt", "w") as f:
    f.write(content)
