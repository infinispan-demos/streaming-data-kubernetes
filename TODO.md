# Add Persistent Volume for binary files
Looking for an example, I never played too much with volume.

https://docs.openshift.com/enterprise/3.0/dev_guide/volumes.html#adding-volumes

Start with an EmptyDir volume (edited)

Create a project with a simple endpoint to upload the file

Mount this volument in the 2 other pods

# Remove big files and compact repo size
Use: https://rtyley.github.io/bfg-repo-cleaner/

# Use indexed instead of distributed cache configurations

# Create caches on the fly with administration API
Requires centralising some initialization code.
Hmmm, having them in config means there's no need for extra execution to show caches.

# Use RxJava for EventBus consumer for inject/stop

# Run web-viewer on OpenShift
