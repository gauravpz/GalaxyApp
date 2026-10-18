# ProGuard / R8 Rules for Galaxy Games Wear OS

# Keep data classes for JSON parsing and DataStore serialization
-keep class com.gaurav.galaxyapp.data.** { *; }

# Keep WorkManager worker for reflection instantiation
-keep class com.gaurav.galaxyapp.worker.ConfigSyncWorker { *; }
