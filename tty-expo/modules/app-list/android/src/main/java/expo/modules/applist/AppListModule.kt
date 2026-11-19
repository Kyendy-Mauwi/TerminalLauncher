package expo.modules.applist

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class AppListModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("AppList")

    // Every activity that appears in the launcher: package name, label, and whether it is a
    // system app. The terminal app itself is excluded.
    AsyncFunction("listApps") {
      val context = appContext.reactContext
      if (context == null) {
        emptyList<Map<String, Any>>()
      } else {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
          @Suppress("DEPRECATION")
          packageManager.queryIntentActivities(intent, 0)
        }

        resolved
          .mapNotNull { info ->
            val application = info.activityInfo?.applicationInfo ?: return@mapNotNull null
            val label = info.loadLabel(packageManager)?.toString() ?: application.packageName
            Triple(application.packageName, label, application)
          }
          .filter { (packageName) -> packageName != context.packageName }
          .distinctBy { (packageName) -> packageName }
          .map { (packageName, label, application) ->
            mapOf(
              "packageName" to packageName,
              "label" to label,
              "system" to ((application.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
            )
          }
          .sortedBy { entry -> (entry["label"] as? String)?.lowercase() ?: "" }
      }
    }

    AsyncFunction("openApp") { packageName: String ->
      val context = appContext.reactContext
      val launchIntent = context?.packageManager?.getLaunchIntentForPackage(packageName)
      if (context == null || launchIntent == null) {
        false
      } else {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        true
      }
    }
  }
}
