package com.github.molcikas.photon.perf;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import com.github.molcikas.photon.perf.photon.Recipe;

import java.lang.reflect.Field;

public class ReflectionTest
{
    private final int TEST_RUNS = 10;
    private final int TEST_ITERATIONS = 10000000;

    @Test
    public void testReflectionNoCache()
    {
        try
        {
            for(int t = 0; t < TEST_RUNS; t++)
            {
                Recipe recipe = new Recipe();

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                for (int i = 0; i < TEST_ITERATIONS; i++)
                {
                    Field field = Recipe.class.getDeclaredField("name");
                    field.setAccessible(true);
                    field.set(recipe, "myname" + i);
                }
                long finishTime = stopWatch.getNanoTime();
                System.out.println(String.format("Finished after %s ms with avg set taking %s ns.", finishTime / 1000000, finishTime / TEST_ITERATIONS));
            }
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testReflectionWithCache()
    {
        try
        {
            for(int t = 0; t < TEST_RUNS; t++)
            {
                Recipe recipe = new Recipe();
                Field field = Recipe.class.getDeclaredField("name");
                field.setAccessible(true);

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                for (int i = 0; i < TEST_ITERATIONS; i++)
                {
                    field.set(recipe, "myname" + i);
                }
                long finishTime = stopWatch.getNanoTime();
                System.out.println(String.format("Finished after %s ms with avg set taking %s ns.", finishTime / 1000000, finishTime / TEST_ITERATIONS));
            }
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
