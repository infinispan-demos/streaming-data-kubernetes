# Add Persistent Volume for binary files
Looking for an example, I never played too much with volume.

https://docs.openshift.com/enterprise/3.0/dev_guide/volumes.html#adding-volumes

Start with an EmptyDir volume (edited)

Create a project with a simple endpoint to upload the file

Mount this volument in the 2 other pods
