import configuration.model.Build;
import configuration.model.Property;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.internal.util.Base64;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.Arrays;

public class TeamcityRestClient {

	private String myBaseAddress;
	private String myAuthToken;
	private final Marshaller marshaller;
	private final JAXBContext context;
	HttpAuthenticationFeature httpAuthenticationFeature;

	public TeamcityRestClient(String baseAddress, String userName, String password) throws JAXBException {
		final Class<Property> type = Property.class;
		context = JAXBContext.newInstance(type.getPackage().getName(), type.getClassLoader(), null);
		marshaller = context.createMarshaller();
		myBaseAddress = baseAddress.concat("app/rest");
		myAuthToken = "Basic "
					  + Arrays.toString(Base64.encode(userName.concat(":").concat(password).getBytes()));
		httpAuthenticationFeature = getAuth(userName, password);
	}

	public Build triggerBuild(Build buildData) {
		return getEntityFromPost("buildQueue", buildData, Build.class, MediaType.APPLICATION_XML_TYPE);
	}

	public Build getBuildInfo(Long buildId) {
		return getEntityFromGet(String.format("builds/%d", buildId), Build.class, MediaType.APPLICATION_XML_TYPE);
	}


	private <T> T getEntityFromGet(String path, final Class<T> entityClass, MediaType mediaType) {
		Response response = prepareClient(path, mediaType).get();
		return response.readEntity(entityClass);
	}


	private <T> T getEntityFromPost(String path, T entity, final Class<T> entityClass, MediaType mediaType) {
		Response response = prepareClient(path, mediaType).post(Entity.entity(entity, mediaType));
		return response.readEntity(entityClass);
	}

	private Invocation.Builder prepareClient(String path, MediaType mediaType) {
		return ClientBuilder.newClient().
				target(myBaseAddress).
				path(path).
				register(httpAuthenticationFeature).
				request(mediaType);
	}


	public HttpAuthenticationFeature getAuth(String username, String password) {
		return HttpAuthenticationFeature.basic(username, password);
	}
}
