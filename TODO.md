# TODOs 

- [x] Add hostPath Persistent Volume for binary files.
- [ ] Consider using emptyDir and rsync persistent volume when local.
Might be needed if running inside Minishift.
Would rsync-ing needed if persistent volume attached to deployment config as opposed to pod?
- [ ] Remove big files and compact repo size.
Use: [bfg-repo-cleaner](https://rtyley.github.io/bfg-repo-cleaner)
- Use indexed instead of distributed cache configurations.
When using indexed, transaction timeout errors appear with current Infinispan version.
Try with latest available version in case it's a bug.
- [ ] Create caches on the fly with administration API?
Requires centralising some initialization code.
Hmmm, having them in config means there's no need for extra execution to show caches.
- [ ] Use RxJava for EventBus consumer for inject/stop (not callback).
- [ ] Run web-viewer on OpenShift.
- [ ] Re-implement visualizer with Vert.x
Is Wildfly really needed for a visualizer?
The visualizer uses JMX for some of the info.
This can sometimes be slow to react. 
We should try to use Hot Rod exclusively.
- [ ] Inject/store data using REST API, simpler to understand.
- [ ] Query data using REST API, simpler to understand.
Keep continuous query API using Java client until there's a REST option
 