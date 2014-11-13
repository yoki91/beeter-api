package edu.upc.eetac.dsa.smachado.beeter.api;

import java.sql.*;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.codec.digest.DigestUtils;
import edu.upc.eetac.dsa.smachado.beeter.api.model.User;


@Path("/users")
public class UserResource 
{	
  private DataSource ds = DataSourceSPA.getInstance().getDataSource();

  private final static String GET_USER_BY_USERNAME_QUERY = "select * from users where username=?";
  private final static String INSERT_USER_INTO_USERS = "insert into users values(?, MD5(?), ?, ?)";
  private final static String INSERT_USER_INTO_USER_ROLES = "insert into user_roles values (?, 'registered')";
  
  
  
  
  
  @Path("/{username}")
  @GET
  @Produces((MediaType.BEETER_API_USER))
  public Response ObtenerUsuarioCache(@PathParam("username") String username,@Context Request request)
  {
	  User Usuario=new User();
	  Usuario=ObtenerUsuarioDesdeDB(username);
	  String referencia = DigestUtils.md5Hex(Usuario.getName()+Usuario.getEmail());
	  CacheControl cc=new CacheControl();
	  
	  
	  EntityTag eTag =new EntityTag(referencia);
	  
	  Response.ResponseBuilder rb =request.evaluatePreconditions(eTag);
	  
	  if(rb !=null)
	  {
		  return rb.cacheControl(cc).tag(eTag).build();
	  }
	  else
	   rb=Response.ok(Usuario).cacheControl(cc).tag(eTag);
	  return rb.build();
	  
	  
  }
  
  private User ObtenerUsuarioDesdeDB(String username)
  {
	  User usuario=new User();
	  Connection conn = null;
		try {
			conn = ds.getConnection();
		} 
		catch (SQLException e) 
		{
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		
		PreparedStatement stmt = null;
		try 
		{
			stmt = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmt.setString(1, username);

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) 
			{
				usuario.setUsername(rs.getString("username"));
				//usuario.setPassword(rs.getString("userpass"));
				usuario.setEmail(rs.getString("email"));
				usuario.setName(rs.getString("name"));
			} else
				throw new NotFoundException(username + " not found.");
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
	  
	  
	  return usuario;
  }
	
	
	
	
	
	
	  
	@POST
	@Consumes(MediaType.BEETER_API_USER)
	@Produces(MediaType.BEETER_API_USER)
	public User createUser(User user) 
	{
		validateUser(user);

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} 
		catch (SQLException e) 
		{
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		PreparedStatement stmtGetUsername = null;
		PreparedStatement stmtInsertUserIntoUsers = null;
		PreparedStatement stmtInsertUserIntoUserRoles = null;
		try 
		{
			stmtGetUsername = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmtGetUsername.setString(1, user.getUsername());

			ResultSet rs = stmtGetUsername.executeQuery();
			if (rs.next())throw new WebApplicationException(user.getUsername()+ " already exists.", Status.CONFLICT);
			rs.close();

			conn.setAutoCommit(false);
			stmtInsertUserIntoUsers = conn.prepareStatement(INSERT_USER_INTO_USERS);
			stmtInsertUserIntoUserRoles = conn.prepareStatement(INSERT_USER_INTO_USER_ROLES);
			stmtInsertUserIntoUsers.setString(1, user.getUsername());
			stmtInsertUserIntoUsers.setString(2, user.getPassword());
			stmtInsertUserIntoUsers.setString(3, user.getName());
			stmtInsertUserIntoUsers.setString(4, user.getEmail());
			stmtInsertUserIntoUsers.executeUpdate();
			stmtInsertUserIntoUserRoles.setString(1, user.getUsername());
			stmtInsertUserIntoUserRoles.executeUpdate();
			conn.commit();
		} 
		catch (SQLException e) 
		{
			if (conn != null)
				try 
			{
					conn.rollback();
				} 
			catch (SQLException e1) 
			{
				}
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} 
		finally 
		{
			try {
				if (stmtGetUsername != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUsers != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUserRoles != null)
					stmtGetUsername.close();
				conn.setAutoCommit(true);
				conn.close();
			} 
			catch (SQLException e) 
			{
				
			}
		}
		user.setPassword(null);
		return user;
	}

	private void validateUser(User user) 
	{
		if (user.getUsername() == null)
			throw new BadRequestException("username cannot be null.");
		if (user.getPassword() == null)
			throw new BadRequestException("password cannot be null.");
		if (user.getName() == null)
			throw new BadRequestException("name cannot be null.");
		if (user.getEmail() == null)
			throw new BadRequestException("email cannot be null.");
	}

	@Path("/login")
	@POST
	@Produces(MediaType.BEETER_API_USER)
	@Consumes(MediaType.BEETER_API_USER)
	public User login(User user) 
	{
		if (user.getUsername() == null || user.getPassword() == null)
			throw new BadRequestException("username and password cannot be null.");

		String pwdDigest = DigestUtils.md5Hex(user.getPassword());
		String storedPwd = getUserFromDatabase(user.getUsername(), true).getPassword();
		user.setLoginSuccessful(pwdDigest.equals(storedPwd));
		user.setPassword(null);
		return user;
	}

	
	private User getUserFromDatabase(String username, boolean password) 
	{
		User user = new User();
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
		try 
		{
			stmt = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmt.setString(1, username);

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) 
			{
				user.setUsername(rs.getString("username"));
				if (password)
				user.setPassword(rs.getString("userpass"));
				user.setEmail(rs.getString("email"));
				user.setName(rs.getString("name"));
			} else
				throw new NotFoundException(username + " not found.");
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
			} catch (SQLException e) 
			{
				
			}
		}

		return user;
	}

}
