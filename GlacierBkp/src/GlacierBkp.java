import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.DeleteVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.DescribeVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;

public class GlacierBkp {
	
	

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		// create the command line parser
		CommandLineParser parser = new GnuParser();

		// create the Options
		Options options = new Options();
		Option operation, endPoint, vaultName, archiveId, filePath, partSize, version;
		
		operation 	= new Option( "op", true, "Operation to perform:\n " +
												"\tar. ArchiveDelete\n" +
												"\tad. ArchiveDownload\n" +
												"\tau. ArchiveUpload\n" +
												"\tvi. VaultInventory\n" +
												"\tvc. VaultCreate\n" +
												"\tvd. VaultDescribe\n" +
												"\tvl. VaultList\n" +
												"\tvr. VaultDelete\n");
		endPoint 	= new Option( "ep", true, "default: glacier.us-west-2.amazonaws.com");
		vaultName 	= new Option( "vn", true, "set a vault name");
		archiveId 	= new Option( "id", true, "set archive ID");
		filePath 	= new Option( "f", true, "File path do get or download archives");
		partSize 	= new Option( "s", true, "Part size for multipart upload. " +
												"Min 1Mb, max 4Gb " +
												"- power of 2 default 10Mb");
		version 	= new Option( "v", false, "GlacierBkp v0.01");
		
		options.addOption(operation);
		options.addOption(endPoint);
		options.addOption(vaultName);
		options.addOption(archiveId);
		options.addOption(filePath);
		options.addOption(partSize);
		options.addOption(version);
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    // mandatory options and common settings
		    AmazonAWSGlacierScript aws = new AmazonAWSGlacierScript();
		    aws.setStrVaultName(line.getOptionValue("vn"));
		    aws.setStrEndPoint(line.getOptionValue("ep", "glacier.us-west-2.amazonaws.com"));
		    aws.getClient().setEndpoint(aws.getStrEndPoint());
		    
		    switch (line.getOptionValue("op")) {
		    
		    case "ar":
		    	// Remove (delete) the archive.
	            aws.getClient().deleteArchive(new DeleteArchiveRequest()
	                .withVaultName(aws.getStrVaultName())
	                .withArchiveId(aws.getStrArchiveId()));     
	            System.out.println("Deleted archive successfully.");
	            break;
	            
		    case "ad":
		    	// Archive Download
		    	aws.setStrArchiveId(line.getOptionValue("id"));
		    	aws.setStrFilePath(line.getOptionValue("f"));
		    	AmazonSQSClient sqsClient = new AmazonSQSClient(aws.getCredentials());
		    	AmazonSNSClient snsClient = new AmazonSNSClient(aws.getCredentials());
		        sqsClient.setEndpoint("sqs.us-west-2.amazonaws.com");
		        snsClient.setEndpoint("sns.us-west-2.amazonaws.com");

	            ArchiveTransferManager atm = new ArchiveTransferManager(aws.getClient(), sqsClient, snsClient);	            
	            atm.download(aws.getStrVaultName(), aws.getStrArchiveId(), new File(aws.getStrFilePath()));
	            break;
		     
		    case "au":
		    	// Archive multi part upload
		    	aws.setStrFilePath(line.getOptionValue("f"));
		    	aws.setStrPartSize(line.getOptionValue("s", "8388608")); // default 8Mb
		    	ArchiveMPU arcMPU = new ArchiveMPU(aws);
	            System.out.println("Uploading an archive.");
	            String uploadId = arcMPU.initiateMultipartUpload();
	            String checksum = arcMPU.uploadParts(uploadId);
	            String arcId = arcMPU.CompleteMultiPartUpload(uploadId, checksum);
	            System.out.println("Completed an archive. ArchiveId: " + arcId);
	            break;
	            
		    case "vi":
		    	// Vault Inventory
		    	aws.setStrFilePath(line.getOptionValue("f"));
		    	aws.setStrVaultName(line.getOptionValue("vn"));
		    	VaultInventory vaultInv = new VaultInventory(aws);
		    	vaultInv.setupSQS();
	            vaultInv.setupSNS();

	            String jobId = vaultInv.initiateJobRequest();
	            System.out.println("Jobid = " + jobId);
	            
	            Boolean success = vaultInv.waitForJobToComplete(jobId, vaultInv.sqsQueueURL);
	            if (!success) { throw new Exception("Job did not complete successfully."); }
	            
	            vaultInv.downloadJobOutput(jobId);
	            
	            vaultInv.cleanUp();
	            break;
	            
		    case "vc":
		    	// Create a new vault
		    	aws.setStrVaultName(line.getOptionValue("vn"));
		    	CreateVaultRequest createVaultRequest = new CreateVaultRequest()
	            .withVaultName(aws.getStrVaultName());
		    	CreateVaultResult createVaultResult = aws.getClient().createVault(createVaultRequest);

		    	System.out.println("Created vault successfully: " + createVaultResult.getLocation());
		    	break;
		    
		    case "vd":
		    	// Vault describe
		    	DescribeVaultRequest describeVaultRequest = new DescribeVaultRequest()
	            .withVaultName(aws.getStrVaultName());
		        DescribeVaultResult describeVaultResult  = aws.getClient().describeVault(describeVaultRequest);
	
		        System.out.println("Describing the vault: " + aws.getStrVaultName());
		        System.out.print(
		                "CreationDate: " + describeVaultResult.getCreationDate() +
		                "\nLastInventoryDate: " + describeVaultResult.getLastInventoryDate() +
		                "\nNumberOfArchives: " + describeVaultResult.getNumberOfArchives() + 
		                "\nSizeInBytes: " + describeVaultResult.getSizeInBytes() + 
		                "\nVaultARN: " + describeVaultResult.getVaultARN() + 
		                "\nVaultName: " + describeVaultResult.getVaultName());
		        break;
		        
		    case "vl":
		    	// Vault list
		    	ListVaultsRequest listVaultsRequest = new ListVaultsRequest();
		        ListVaultsResult listVaultsResult = aws.getClient().listVaults(listVaultsRequest);

		        List<DescribeVaultOutput> vaultList = listVaultsResult.getVaultList();
		        System.out.println("\nDescribing all vaults (vault list):");
		        for (DescribeVaultOutput vault : vaultList) {
		            System.out.println(
		                    "\nCreationDate: " + vault.getCreationDate() +
		                    "\nLastInventoryDate: " + vault.getLastInventoryDate() +
		                    "\nNumberOfArchives: " + vault.getNumberOfArchives() + 
		                    "\nSizeInBytes: " + vault.getSizeInBytes() + 
		                    "\nVaultARN: " + vault.getVaultARN() + 
		                    "\nVaultName: " + vault.getVaultName()); 
		        }
		        break;
		        
		    case "vr":
		    	// Vault delete
		    	DeleteVaultRequest request = new DeleteVaultRequest()
	            .withVaultName(aws.getStrVaultName());
		    	aws.getClient().deleteVault(request);
		    	System.out.println("Deleted vault: " + aws.getStrVaultName());
		    	break;
		    }
		    
		    
		}
		catch( Exception exp ) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar GlacieBkp", options);
		    System.out.println( "Exception:" + exp.getMessage() );
		}

	}

}
