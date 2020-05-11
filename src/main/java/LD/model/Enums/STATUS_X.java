package LD.model.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum STATUS_X
{
	X;

	@JsonCreator
	public static STATUS_X fromString(String string)
	{
		if(string.equals("X"))
		{
			return STATUS_X.X;
		}
		else
		{
			return null;
		}
	}
}
