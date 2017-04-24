package com.github.molcikas.photon.tests.unit.entities.fieldtest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

public class FieldTest
{
    private int id;
    private Date date;
    private ZonedDateTime zonedDateTime;
    private LocalDate localDate;
    private LocalDateTime localDateTime;
    private Instant instant;
    private TestEnum testEnumNumber;
    private TestEnum testEnumString;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public ZonedDateTime getZonedDateTime()
    {
        return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime)
    {
        this.zonedDateTime = zonedDateTime;
    }

    public LocalDate getLocalDate()
    {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate)
    {
        this.localDate = localDate;
    }

    public TestEnum getTestEnumNumber()
    {
        return testEnumNumber;
    }

    public void setTestEnumNumber(TestEnum testEnumNumber)
    {
        this.testEnumNumber = testEnumNumber;
    }

    public TestEnum getTestEnumString()
    {
        return testEnumString;
    }

    public void setTestEnumString(TestEnum testEnumString)
    {
        this.testEnumString = testEnumString;
    }

    public LocalDateTime getLocalDateTime()
    {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime)
    {
        this.localDateTime = localDateTime;
    }

    public Instant getInstant()
    {
        return instant;
    }

    public void setInstant(Instant instant)
    {
        this.instant = instant;
    }

    public FieldTest()
    {
    }

    public FieldTest(int id,
                     Date date,
                     ZonedDateTime zonedDateTime,
                     LocalDate localDate,
                     LocalDateTime localDateTime,
                     Instant instant,
                     TestEnum testEnumNumber,
                     TestEnum testEnumString)
    {
        this.id = id;
        this.date = date;
        this.zonedDateTime = zonedDateTime;
        this.localDate = localDate;
        this.localDateTime = localDateTime;
        this.instant = instant;
        this.testEnumNumber = testEnumNumber;
        this.testEnumString = testEnumString;
    }
}
