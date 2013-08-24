package br.com.indra.aws;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadResult;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadResult;
import com.amazonaws.services.glacier.model.UploadMultipartPartRequest;
import com.amazonaws.services.glacier.model.UploadMultipartPartResult;
import com.amazonaws.util.BinaryUtils;

public class ArchiveMPU {
    
	private AmazonAWSGlacierScript aws;
	
    public ArchiveMPU(AmazonAWSGlacierScript aws) throws IOException {
    	
    	this.aws = aws;

    }
    
    public String initiateMultipartUpload() {
        // Initiate
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest()
            .withVaultName(aws.getStrVaultName())
            .withArchiveDescription("my archive " + (new Date()))
            .withPartSize(aws.getStrPartSize());            
        
        InitiateMultipartUploadResult result = aws.getClient().initiateMultipartUpload(request);
        
        System.out.println("ArchiveID: " + result.getUploadId());
        return result.getUploadId();
    }

    public String uploadParts(String uploadId) throws AmazonServiceException, NoSuchAlgorithmException, AmazonClientException, IOException {

        int filePosition = 0;
        long currentPosition = 0;
        byte[] buffer = new byte[Integer.valueOf(aws.getStrPartSize())];
        List<byte[]> binaryChecksums = new LinkedList<byte[]>();
        
        File file = new File(aws.getStrFilePath());
        @SuppressWarnings("resource")
		FileInputStream fileToUpload = new FileInputStream(file);
        String contentRange;
        int read = 0;
        while (currentPosition < file.length())
        {
            read = fileToUpload.read(buffer, filePosition, buffer.length);
            if (read == -1) { break; }
            byte[] bytesRead = Arrays.copyOf(buffer, read);

            contentRange = String.format("bytes %s-%s/*", currentPosition, currentPosition + read - 1);
            String checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
            byte[] binaryChecksum = BinaryUtils.fromHex(checksum);
            binaryChecksums.add(binaryChecksum);
            System.out.println(contentRange);
                        
            //Upload part.
            UploadMultipartPartRequest partRequest = new UploadMultipartPartRequest()
            .withVaultName(aws.getStrVaultName())
            .withBody(new ByteArrayInputStream(bytesRead))
            .withChecksum(checksum)
            .withRange(contentRange)
            .withUploadId(uploadId);               
        
            UploadMultipartPartResult partResult = aws.getClient().uploadMultipartPart(partRequest);
            System.out.println("Part uploaded, checksum: " + partResult.getChecksum());
            
            currentPosition = currentPosition + read;
        }
        String checksum = TreeHashGenerator.calculateTreeHash(binaryChecksums);
        return checksum;
    }

    public String CompleteMultiPartUpload(String uploadId, String checksum) throws NoSuchAlgorithmException, IOException {
        
        File file = new File(aws.getStrFilePath());

        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest()
            .withVaultName(aws.getStrVaultName())
            .withUploadId(uploadId)
            .withChecksum(checksum)
            .withArchiveSize(String.valueOf(file.length()));
        
        CompleteMultipartUploadResult compResult = aws.getClient().completeMultipartUpload(compRequest);
        return compResult.getLocation();
    }
}