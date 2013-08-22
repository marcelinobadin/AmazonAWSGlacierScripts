import java.io.IOException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;


public class AmazonAWSGlacierScript {

	public String strVaultName;
	public String strEndPoint;
	public String strArchiveId;
	public String strFilePath;
	public String strPartSize;
	public static AmazonGlacierClient client;
	private static AWSCredentials credentials;
	
	public String getStrVaultName() {
		return strVaultName;
	}

	public void setStrVaultName(String strVaultName) {
		this.strVaultName = strVaultName;
	}

	public String getStrEndPoint() {
		return strEndPoint;
	}

	public void setStrEndPoint(String strEndPoint) {
		this.strEndPoint = strEndPoint;
	}

	public String getStrArchiveId() {
		return strArchiveId;
	}

	public void setStrArchiveId(String strArchiveId) {
		this.strArchiveId = strArchiveId;
	}

	public String getStrFilePath() {
		return strFilePath;
	}

	public void setStrFilePath(String strFilePath) {
		this.strFilePath = strFilePath;
	}

	public String getStrPartSize() {
		return strPartSize;
	}

	public void setStrPartSize(String strPartSize) {
		this.strPartSize = strPartSize;
	}
	
	public AmazonGlacierClient getClient() {
		return client;
	}

	public static void setClient(AmazonGlacierClient client) {
		AmazonAWSGlacierScript.client = client;
	}

	public AWSCredentials getCredentials() {
		return credentials;
	}

	private static void setCredentials(AWSCredentials credentials) {
		AmazonAWSGlacierScript.credentials = credentials;
	}

	public AmazonAWSGlacierScript() {
		
		try {
			AmazonAWSGlacierScript.setCredentials(new PropertiesCredentials(
			        AmazonAWSGlacierScript.class.getResourceAsStream("AwsCredentials.properties")));
	        AmazonAWSGlacierScript.setClient(new AmazonGlacierClient(credentials));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
