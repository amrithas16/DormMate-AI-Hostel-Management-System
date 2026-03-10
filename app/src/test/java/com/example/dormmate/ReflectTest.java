package com.example.dormmate;

import org.junit.Test;
import com.google.ai.client.generativeai.GenerativeModel;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

public class ReflectTest {
    @Test
    public void testPrintConstructors() {
        StringBuilder sb = new StringBuilder();
        for (Constructor<?> c : GenerativeModel.class.getConstructors()) {
            sb.append("Constructor:\n");
            for (Parameter p : c.getParameters()) {
                sb.append("  ").append(p.getType().getName()).append("\n");
            }
        }
        throw new RuntimeException("CONSTRUCTORS:\n" + sb.toString());
    }
}
