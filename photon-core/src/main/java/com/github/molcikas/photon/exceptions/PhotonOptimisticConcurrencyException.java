package com.github.molcikas.photon.exceptions;

public class PhotonOptimisticConcurrencyException extends RuntimeException
{
    public PhotonOptimisticConcurrencyException()
    {
        super("An optimistic concurrency exception occurred.");
    }
}
