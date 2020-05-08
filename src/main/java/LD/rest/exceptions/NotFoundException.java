package LD.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException
{
	String message;

	public NotFoundException(String message)
	{
		this.message = message;
	}

	public NotFoundException()
	{
		this.message = new String();
	}
}
