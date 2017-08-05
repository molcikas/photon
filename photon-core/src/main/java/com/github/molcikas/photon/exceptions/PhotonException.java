package com.github.molcikas.photon.exceptions;

public class PhotonException extends RuntimeException
{
    public PhotonException(String message, Object... args)
    {
        super(String.format(message, args));
    }

    public PhotonException(Throwable cause, String message, Object... args)
    {
        super(String.format(message, args), cause);
    }
}
