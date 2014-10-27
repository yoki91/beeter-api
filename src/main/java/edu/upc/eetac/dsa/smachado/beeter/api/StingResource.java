package edu.upc.eetac.dsa.smachado.beeter.api;
import java.sql.*;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import edu.upc.eetac.dsa.smachado.beeter.api.model.Sting;
import edu.upc.eetac.dsa.smachado.beeter.api.model.StingCollection;

@Path("/stings")
public class StingResource 
{
	@Context
	private SecurityContext security;
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();
	private String GET_STINGS_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp < ifnull(?, now()) order by creation_timestamp desc limit ?";
	private String GET_STINGS_QUERY_FROM_LAST = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp > ? order by creation_timestamp desc";
	@GET
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	public StingCollection getStings(@QueryParam("length") int length,@QueryParam("before") long before, @QueryParam("after") long after) 
	{
		StingCollection stings = new StingCollection();

		Connection conn = null;
		try 
		{
			conn = ds.getConnection();
		} catch (SQLException e) 
		{
			throw new ServerErrorException("Could not connect to the database",Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			boolean updateFromLast = after > 0;
			stmt = updateFromLast ? conn.prepareStatement(GET_STINGS_QUERY_FROM_LAST) : conn.prepareStatement(GET_STINGS_QUERY);
			if (updateFromLast) 
			{
				stmt.setTimestamp(1, new Timestamp(after));
			} else 
			{
				if (before > 0)
					stmt.setTimestamp(1, new Timestamp(before));
				else
					stmt.setTimestamp(1, null);
				length = (length <= 0) ? 5 : length;
				stmt.setInt(2, length);
			}
			ResultSet rs = stmt.executeQuery();
			boolean first = true;
			long oldestTimestamp = 0;
			while (rs.next()) 
			{
				Sting sting = new Sting();
				sting.setStingid(rs.getInt("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				oldestTimestamp = rs.getTimestamp("last_modified").getTime();
				sting.setLastModified(oldestTimestamp);
				if (first) 
				{
					first = false;
					stings.setNewestTimestamp(sting.getLastModified());
				}
				stings.addSting(sting);
			}
			stings.setOldestTimestamp(oldestTimestamp);
		} catch (SQLException e) 
		{
			throw new ServerErrorException(e.getMessage(),Response.Status.INTERNAL_SERVER_ERROR);
		} 
		finally 
		{
			try 
			{
				if (stmt != null)
					stmt.close();
				conn.close();
			} 
			catch (SQLException e) 
			{
				
			}
		}

		return stings;
	}
	
	
	
	
	
	
	
	
	
	
	private String BUSCAR_STING_POR_SUBJETOYCONTENIDO_QUERY="select s.* from stings s where s.subject=? and s.content=?";
	
	
	
	
	
	
	@GET
	@Path("/search")
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	
	public StingCollection BuscarStings(@QueryParam("subject") String subject,@QueryParam("content") String content)
	{
		
		

		StingCollection stings = new StingCollection();
		Sting sting = new Sting();
		Connection conn = null;
		try
		{
			conn = ds.getConnection();
		} catch (SQLException e) 
		{
			throw new ServerErrorException("Could not connect to the database",Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try 
		{
			stmt=conn.prepareStatement(BUSCAR_STING_POR_SUBJETOYCONTENIDO_QUERY);
			stmt.setString(1,subject);
			stmt.setString(2, content);
			System.out.print(stmt);
			ResultSet rs = stmt.executeQuery();
			System.out.println("antes if");	
			if(rs.next()) //if existe el resurso en Mysql ejecuta el bucle
			{
				//while(rs.next())  
				System.out.println("dentro if");
				Sting sting1=new Sting();
				sting1.setStingid(rs.getInt("stingid"));
				sting1.setUsername(rs.getString("username"));
				sting1.setSubject(rs.getString("subject"));
				sting1.setContent(rs.getString("content"));
				sting1.setLastModified(rs.getTimestamp("last_modified").getTime());
				sting1.setCreationTimestamp(rs.getTimestamp("creation_timestamp").getTime());
				stings.addSting(sting1);
			}		

			else
			{
				throw new NotFoundException("There's no sting with subject"+sting.getSubject()+ "and content"+sting.getContent());
			}
		}
		catch (SQLException e) 
		{
			throw new ServerErrorException(e.getMessage(),Response.Status.INTERNAL_SERVER_ERROR);
		} 
		finally 
		{
			try 
			{
				if (stmt != null)
					stmt.close();
				conn.close();
			} 
			catch (SQLException e) 
			{
				
			}
		}
		
		return stings;
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	private String GET_STING_BY_ID_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.stingid=?";

	private Sting getStingFromDatabase(String stingid) 
	{
		Sting sting = new Sting();

		Connection conn = null;
		try
		{
			conn = ds.getConnection();
		} 
		catch (SQLException e) 
		{
			throw new ServerErrorException("Could not connect to the database",Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_STING_BY_ID_QUERY);
			stmt.setInt(1, Integer.valueOf(stingid));
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
			{
				sting.setStingid(rs.getInt("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				sting.setContent(rs.getString("content"));
				sting.setLastModified(rs.getTimestamp("last_modified").getTime());
				sting.setCreationTimestamp(rs.getTimestamp("creation_timestamp").getTime());
			} 
			else 
			{
				throw new NotFoundException("There's no sting with stingid="+ stingid);
			}
		} 
		catch (SQLException e) 
		{
			throw new ServerErrorException(e.getMessage(),Response.Status.INTERNAL_SERVER_ERROR);
		} 
		finally 
		{
			try 
			{
				if (stmt != null)
					stmt.close();
				conn.close();
			} 
			catch (SQLException e) 
			{
				
			}
		}

		return sting;
	}
	
	
	@GET
	@Path("/{stingid}")
	@Produces(MediaType.BEETER_API_STING)
	public Response getSting(@PathParam("stingid") String stingid,@Context Request request) 
	{
		// Create CacheControl
		CacheControl cc = new CacheControl();

		Sting sting = getStingFromDatabase(stingid);

		// Calculate the ETag on last modified date of user resource
		EntityTag eTag = new EntityTag(Long.toString(sting.getLastModified()));

		// Verify if it matched with etag available in http request
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);

		// If ETag matches the rb will be non-null;
		// Use the rb to return the response without any further processing
		if (rb != null) {
			return rb.cacheControl(cc).tag(eTag).build();
		}

		// If rb is null then either it is first time request; or resource is
		// modified
		// Get the updated representation and return with Etag attached to it
		rb = Response.ok(sting).cacheControl(cc).tag(eTag);

		return rb.build();
	}

	private String INSERT_STING_QUERY = "insert into stings (username, subject, content) value (?, ?, ?)";

	@POST
	@Consumes(MediaType.BEETER_API_STING)
	@Produces(MediaType.BEETER_API_STING)
	public Sting createSting(Sting sting) 
	{
		validateSting(sting);
		Connection conn = null;
		try 
		{
			conn = ds.getConnection();
		} catch (SQLException e) 
		{
			throw new ServerErrorException("Could not connect to the database",Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try 
		{
			stmt = conn.prepareStatement(INSERT_STING_QUERY,Statement.RETURN_GENERATED_KEYS);

			stmt.setString(1, security.getUserPrincipal().getName());
			stmt.setString(2, sting.getSubject());
			stmt.setString(3, sting.getContent());
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) 
			{
				int stingid = rs.getInt(1);
				sting = getStingFromDatabase(Integer.toString(stingid));
			} 
			else 
			{
				// Something has failed...
			}
		} 
		catch (SQLException e) 
		{
			throw new ServerErrorException(e.getMessage(),
			Response.Status.INTERNAL_SERVER_ERROR);
		} 
		finally 
		{
			try 
			{
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) 
			{

			}
		}

		return sting;
	}

	private void validateSting(Sting sting) 
	{
		if (sting.getSubject() == null)
			throw new BadRequestException("Subject can't be null.");
		if (sting.getContent() == null)
			throw new BadRequestException("Content can't be null.");
		if (sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

	
	
	
	private String DELETE_STING_QUERY = "delete from stings where stingid=?";

	@DELETE
	@Path("/{stingid}")
	public void deleteSting(@PathParam("stingid") String stingid) 
	{
		validateUser(stingid);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) 
		{
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(DELETE_STING_QUERY);
			stmt.setInt(1, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();
			if (rows == 0) 
			{
				throw new NotFoundException("There's no sting with stingid="+ stingid);
			}
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {

			}
		}
	}

	private String UPDATE_STING_QUERY = "update stings set subject=ifnull(?, subject), content=ifnull(?, content) where stingid=?";

	private void validateUser(String stingid) 
	{
		Sting sting = getStingFromDatabase(stingid);
		String username = sting.getUsername();
		if (!security.getUserPrincipal().getName().equals(username)) 
		{
			throw new ForbiddenException(
					"You are not allowed to modify this sting.");
		}
	}

	@PUT
	@Path("/{stingid}")
	@Consumes(MediaType.BEETER_API_STING)
	@Produces(MediaType.BEETER_API_STING)
	public Sting updateSting(@PathParam("stingid") String stingid, Sting sting) 
	{
		validateUser(stingid);
		validateUpdateSting(sting);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(UPDATE_STING_QUERY);
			stmt.setString(1, sting.getSubject());
			stmt.setString(2, sting.getContent());
			stmt.setInt(3, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();
			if (rows == 1)
				sting = getStingFromDatabase(stingid);
			else {
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);
			}

		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {

			}
		}

		return sting;
	}

	private void validateUpdateSting(Sting sting) {
		if (sting.getSubject() != null && sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent() != null && sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

}
