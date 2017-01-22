package photon.tests.databaseintegrations.h2;

public class H2TestUtil
{
    private static final String h2Url = "jdbc:h2:mem:test;MODE=%s;DB_CLOSE_DELAY=-1";
    public static final String h2User = "sa";
    public static final String h2Password = "";

    public static final String getH2Url(String mode)
    {
        return String.format(h2Url, mode);
    }
}
