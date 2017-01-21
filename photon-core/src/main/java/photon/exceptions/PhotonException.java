package photon.exceptions;

public class PhotonException extends RuntimeException
{
    public PhotonException(String message)
    {
        super(message);
    }

    public PhotonException(String message, Exception cause)
    {
        super(message, cause);
    }
}
