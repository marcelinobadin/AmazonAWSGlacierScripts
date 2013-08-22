AmazonAWSGlacierScripts
=======================

### Amazon AWS Glacier utility tool (command line)

GlacierBkp is a script to do some vault and archive operations.  

In the root directory where you will run GlacierBkp you must create a file AwsCredentials.properties where you will insert your accesskey and secretkey from your Amazon AWS account.

AwsCredentials.properties  
    #Insert your AWS Credentials from http://aws.amazon.com/security-credentials  
    #Tue Aug 20 16:08:15 BRT 2013  
    secretKey=GN2H9qShz7/ovZ69jvirL++26rTF2DGfXEly/iH  
    accessKey=AKIAIVQEWEJY5MN45GH

Usage: java -jar GlacierBkp options  
example: java -jar GlacierBkp -op au -vn yourVault -ep glacier.us-west-2.amazonaws.com -f dir/filename -s 1048576

-op <arg>
ar. ArchiveDelete (mandatory options vn; ep; id)  
ad. ArchiveDownload (1;2;3;5)  
au. ArchiveUpload (1;2;4;6)  
vi. VaultInventory (1;2)  
vc. VaultCreate (1;2)  
vd. VaultDescribe (1;2)  
vl. VaultList (2)  
vr. VaultDelete (1;2)  
-vn Vault Name  
-ep End Point  
-id ArchiveId  
-f File Path  
-s Part Size  
