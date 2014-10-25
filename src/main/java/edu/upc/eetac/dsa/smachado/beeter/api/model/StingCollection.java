package edu.upc.eetac.dsa.smachado.beeter.api.model;

import edu.upc.eetac.dsa.smachado.beeter.api.model.Sting;

import java.util.ArrayList;
import java.util.List;

public class StingCollection 
{
	private long NewestTimestamp;
	private long oldestTimestamp;

	public long getOldestTimestamp() 
	{
		return oldestTimestamp;
	}

	public void setOldestTimestamp(long oldestTimestamp) 
	{
		this.oldestTimestamp = oldestTimestamp;
	}

	private List<Sting> stings;

	public StingCollection() {
		super();
		stings = new ArrayList<Sting>();
	}

	/*public List<Sting> getBuscarStings() 
	{
		return stings;
	}

	public void setBuscarStings(List<Sting> Stings) 
	{
		this.stings = Stings;
	}*/
	
	
	
	public List<Sting> getStings() 
	{
		return stings;
	}

	public void setStings(List<Sting> Stings) 
	{
		this.stings = Stings;
	}
	
	
	

	public void addSting(Sting Stings) 
	{
		stings.add(Stings);
	}

	public long getNewestTimestamp() {
		return NewestTimestamp;
	}

	public void setNewestTimestamp(long newestTimestamp) 
	{
		NewestTimestamp = newestTimestamp;
	}
}
