# workmanager
A sample application has work manager how to code snippets 
Introduction

WorkManager is the recommended Android API for persisting backgrounds tasks during app restarts or device reboots it can be useful when sending analytics data, syncing application data to server or any other long task need to be resumed when app closed or device restarted so its not recommended for background tasks that could be completed or safely terminated during app lifecycle with no need for resuming these tasks to complete work again.

WorkManager handles three types of persistent work Immediate, Long Running and Deferrable

 Getting Started

 Adding dependency to app build.gradle

dependencies {
    implementation "androidx.work:work-runtime-ktx:2.7.1"
}

You can find latest release version here  WorkManager  |  Releases 

Extending Worker abstract class and implement doWork with the work you need to persist. 

class DownloadImageWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
    
        // Do the work here in this case.
        ....

        // Indicate whether the work finished successfully with the Result
        // could be other Result upon purpose Result.failure(), Result.retry()
        return Result.success() 
    }

Build and use WorkRequest (and its subclasses) to define how and when the work should be run.

val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadImageWorker>().build()

Submit downloadWorkRequest object to WorkManager instance to handle it.

WorkManager.getInstance(myContext).enqueue(downloadWorkRequest)

So in the above code user can exit app and close it from background after work request being enqueued  while work not finished yet and when back to app foreground work request can resume work again on same request without the need to resending the request.

 Expedited work request

Expedited work suits tasks which are important to the user or are user-initiated that can complete within a few minutes like sending an email with attachment, making a payment or sending chat message.

here is how to mark the work request to be expedited:

private val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadImageWorker>()
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

In the above snippet, if the app does not have any expedited job quota, the expedited work request will fallback to a regular work request like the one declared in the Introduction code snippets.

Also if the system load is too high, when too many tasks are running, or when the system doesn't have enough memory the expedited task will be deferred until system owns enough memory to start.

but if we set policy toOutOfQuotaPolicy.DROP_WORK_REQUEST,When the app does not have any expedited job quota, the expedited work request will be dropped and no work requests are enqueued.

After expediting work request, contrary to regular work request, work manager will execute the work immediately when app moves to background but execution time is limited to quota given by the system to each app you can learn more about it in this link   https://developer.android.com/topic/performance/appstandby 

 Schedule periodic work request

In some cases you may want to have periodic tasks in your app, you can run periodic work request with minimum interval 15 min also you can add a flex time which is a time slot to start the work within the repeat interval and its starting time will be equal (repeatInterval minus flexInterval)

val myUploadWork = PeriodicWorkRequestBuilder<DownloadImageWorker>(
       1, TimeUnit.HOURS, // repeatInterval (the period cycle)
       15, TimeUnit.MINUTES) // flexInterval
    .build()()

 Work constraints

You can add constraints to a periodic work requests, so when work due period reached the work will not not run until all constraints met or skipped to the next work due period.

And in case a one time work request has unmet constraints, work will be stopped by WorkManager and will be retried when all constraints met.

Types of constraints: NetworkType, BatteryNotLow, Charging status, StorageNotLow, DeviceIdle

Here is an example of how to pass work constraints to work request:

val constraints = Constraints.Builder()
   .setRequiredNetworkType(NetworkType.UNMETERED)
   .setRequiresCharging(true)
   .build()
val myWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<MyWork>()
       .setConstraints(constraints)
       .build()

 Observing work result

By adding tag to your work requests so you can simply observe live data of their results or or cancel them all WorkManager.cancelAllWorkByTag(WORK_TAG)

Or by enqueuing a single work request with a unique name WorkManager.enqueueUniqueWork(), WorkManager.enqueueUniquePeriodicWork() with name conflict policy in case of work request is running with same name.

private val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadImageWorker>()
    .addTag(WORK_TAG).build()
private val saveToStorageWorkRequest = OneTimeWorkRequestBuilder<SaveImageWorker>()
    .addTag(WORK_TAG).build()
WorkManager.getInstance(this).enqueueUniqueWork("sendLogs",..

 You can observe live data by tag or by unique name:

workManager.getWorkInfosByTagLiveData(WORK_TAG)
    .observe(this) { workInfosList ->        
    ....
    }

 Other capabilities  

You can add initial delay for regular/periodic work request .setInitialDelay(10, TimeUnit.MINUTES) for periodic requests delay will be applied only on first repeat interval.

You can return Result.retry() from your worker in case of work failure so your work could be retried. According to retry policy, retries intervals calculated also a minimum wait delay must be set before first retry.

You can set a data which your worker depends on it

Setting data to work request:

val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadImageWorker>()
    .setInputData(
        workDataOf("IMAGE_URL" to "http://..")
    ).build()

Retrieving data:  

override suspend fun doWork(): Result {
        val imageUriInput = inputData.getString("IMAGE_URL") ?: "" return Result.failure()

Work manager supports long running tasks which exceeds 10 min to finish, in this case your work must give a signal to the system to say that “keep this process a live if possible“ while it executing, common use case for that syncing an app depending on a large offline data, so your work should override getForegroundInfo() and call setForeground(getForegroundInfo()) so work manager will shows a configurable notification and run foreground service for the work.

 Chaining work requests

You can chaining multiple work requests so a work request wait for one or many requests to sucess then it can be start running with their results.

In this example saveToStorageWorkRequest will remain in Blocked state until downloadWorkRequest is succeeded with result. if downloadWorkRequest is failed or cancelled then also saveToStorageWorkRequest will be finished by the same state.

WorkManager.getInstance(myContext)
   // work requests to run in parallel
   .beginWith(listOf(downloadWorkRequest))
   .then(saveToStorageWorkRequest)
   .enqueue()

 Conclusion

In the article I tried to walk through the main important capabilities and usages of WorkManager and to list the different ways to persist important work using it, also I added a sample application on Github that contains the most important utilization of WorkManager capabilities
