package a_lexico_robot;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class GeneradorLua {

    public static String generarCodigoLua(List<Integer> valores) {
        System.out.println("=== GeneradorLua: Generando para Robot Sawyer - Secuencia Base→Brazo→Hombro→Garra ===");
        System.out.println("Valores recibidos: " + valores);

        if (valores.size() < 8) {
            System.out.println("ERROR: Insuficientes valores. Se necesitan al menos 8 valores, recibidos: " + valores.size());
            return "-- Error: Se necesitan al menos 8 valores para las 4 articulaciones\n-- Valores recibidos: " + valores.size();
        }

        // Analizar el patrón de los datos
        PatronDetectado patron = analizarPatronDatos(valores);
        System.out.println("Patrón detectado: " + patron.descripcion);

        StringBuilder lua = new StringBuilder();

        // Encabezado
        lua.append("-- Script generado automáticamente para Robot Sawyer\n");
        lua.append("-- Compilador Java - Fecha: ").append(new java.util.Date()).append("\n");
        lua.append("-- Valores procesados: ").append(valores.size()).append("\n");
        lua.append("-- Patrón detectado: ").append(patron.descripcion).append("\n");
        lua.append("-- SECUENCIA: Base(J0) → Brazo(J2) → Hombro(J1) → Garra(J6)\n\n");

        // Requires y función moveToConfig
        lua.append("sim=require'sim'\n\n");
        lua.append("function moveToConfig(handles,maxVel,maxAccel,maxJerk,targetConf)\n");
        lua.append("    local params = {\n");
        lua.append("        joints = handles,\n");
        lua.append("        targetPos = targetConf,\n");
        lua.append("        maxVel = maxVel,\n");
        lua.append("        maxAccel = maxAccel,\n");
        lua.append("        maxJerk = maxJerk,\n");
        lua.append("    }\n");
        lua.append("    sim.moveToConfig(params)\n");
        lua.append("end\n\n");

        // Función para mover articulación específica
        lua.append("function moverArticulacion(handles, indiceArticulacion, anguloGrados, velocidadPorcentaje, descripcion)\n");
        lua.append("    print('🤖 ' .. descripcion .. ': ' .. anguloGrados .. '° (velocidad: ' .. velocidadPorcentaje .. '%)')\n");
        lua.append("    \n");
        lua.append("    -- Validar rangos\n");
        lua.append("    if velocidadPorcentaje <= 0 then\n");
        lua.append("        print('⚠️ Velocidad inválida, usando velocidad mínima')\n");
        lua.append("        velocidadPorcentaje = 10\n");
        lua.append("    end\n");
        lua.append("    \n");
        lua.append("    local anguloRadianes = anguloGrados * math.pi / 180\n");
        lua.append("    \n");
        lua.append("    -- Obtener posición actual\n");
        lua.append("    local posicionActual = {}\n");
        lua.append("    for i=1,7 do\n");
        lua.append("        posicionActual[i] = sim.getJointPosition(handles[i])\n");
        lua.append("    end\n");
        lua.append("    \n");
        lua.append("    -- Actualizar solo la articulación específica\n");
        lua.append("    posicionActual[indiceArticulacion] = anguloRadianes\n");
        lua.append("    \n");
        lua.append("    -- Velocidades base conservadoras\n");
        lua.append("    local factorVelocidad = math.max(0.1, velocidadPorcentaje / 100.0)\n");
        lua.append("    local maxVel = {\n");
        lua.append("        30*math.pi/180 * factorVelocidad,  -- Base\n");
        lua.append("        25*math.pi/180 * factorVelocidad,  -- Hombro\n");
        lua.append("        35*math.pi/180 * factorVelocidad,  -- Brazo\n");
        lua.append("        30*math.pi/180 * factorVelocidad,  -- J3\n");
        lua.append("        40*math.pi/180 * factorVelocidad,  -- J4\n");
        lua.append("        40*math.pi/180 * factorVelocidad,  -- J5\n");
        lua.append("        50*math.pi/180 * factorVelocidad   -- Garra\n");
        lua.append("    }\n");
        lua.append("    \n");
        lua.append("    local maxAccel = {15*math.pi/180,15*math.pi/180,15*math.pi/180,15*math.pi/180,15*math.pi/180,15*math.pi/180,15*math.pi/180}\n");
        lua.append("    local maxJerk = {30*math.pi/180,30*math.pi/180,30*math.pi/180,30*math.pi/180,30*math.pi/180,30*math.pi/180,30*math.pi/180}\n");
        lua.append("    \n");
        lua.append("    moveToConfig(handles, maxVel, maxAccel, maxJerk, posicionActual)\n");
        lua.append("    \n");
        lua.append("    -- Tiempo de espera proporcional\n");
        lua.append("    local tiempoEspera = math.max(1.0, 2.0 / factorVelocidad)\n");
        lua.append("    sim.wait(tiempoEspera)\n");
        lua.append("end\n\n");

        // Función principal
        lua.append("function sysCall_thread()\n");
        lua.append("    print('=== Robot Sawyer - Simulación de Manipulación de Objetos ===')\n");
        lua.append("    print('Patrón: ").append(patron.descripcion).append("')\n");
        lua.append("    \n");

        // Inicialización
        lua.append("    -- Inicialización del robot Sawyer\n");
        lua.append("    local jointHandles = {}\n");
        lua.append("    for i=1,7,1 do\n");
        lua.append("        jointHandles[i] = sim.getObject('../joint', {index=i-1})\n");
        lua.append("    end\n");
        lua.append("    \n");

        // Deshabilitar cámaras
        lua.append("    -- Deshabilitar cámaras\n");
        lua.append("    headCameraHandle = sim.getObject('../head_camera')\n");
        lua.append("    sim.setExplicitHandling(headCameraHandle, 1)\n");
        lua.append("    wristCameraHandle = sim.getObject('../wristCamera')\n");
        lua.append("    sim.setExplicitHandling(wristCameraHandle, 1)\n");
        lua.append("    \n");

        // Posición inicial más conservadora
        lua.append("    print('✅ Robot inicializado - Moviendo a posición inicial')\n");
        lua.append("    local posicionInicial = {0, 0, 0, 0, 0, 0, 0}\n");
        lua.append("    local maxVelInicial = {20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180}\n");
        lua.append("    local maxAccelInicial = {10*math.pi/180,10*math.pi/180,10*math.pi/180,10*math.pi/180,10*math.pi/180,10*math.pi/180,10*math.pi/180}\n");
        lua.append("    local maxJerkInicial = {20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180,20*math.pi/180}\n");
        lua.append("    moveToConfig(jointHandles, maxVelInicial, maxAccelInicial, maxJerkInicial, posicionInicial)\n");
        lua.append("    sim.wait(2.0)\n");
        lua.append("    \n");

        // Generar secuencia
        lua.append(generarSecuenciaSegunPatron(valores, patron));

        lua.append("    print('🎯 ¡Secuencia de manipulación completada!')\n");
        lua.append("end\n\n");

        // Función de limpieza
        lua.append("function sysCall_cleanup()\n");
        lua.append("    print('=== Script del Robot Sawyer terminado ===')\n");
        lua.append("end\n");

        System.out.println("✓ Código Lua generado - Longitud: " + lua.length() + " caracteres");
        return lua.toString();
    }

    // Clase para describir el patrón detectado
    private static class PatronDetectado {

        boolean esRepeticion;
        int numeroRepeticiones;
        boolean tieneIdeaYVuelta;
        String descripcion;
        List<Integer> grupoRepeticion;

        PatronDetectado(boolean esRepeticion, int numeroRepeticiones, boolean tieneIdeaYVuelta,
                String descripcion, List<Integer> grupoRepeticion) {
            this.esRepeticion = esRepeticion;
            this.numeroRepeticiones = numeroRepeticiones;
            this.tieneIdeaYVuelta = tieneIdeaYVuelta;
            this.descripcion = descripcion;
            this.grupoRepeticion = grupoRepeticion;
        }
    }

    private static PatronDetectado analizarPatronDatos(List<Integer> valores) {
        System.out.println("=== Analizando patrón de datos ===");

        // Detectar repeticiones
        RepeticionInfo repeticion = detectarRepeticionCompleta(valores);

        if (repeticion.esRepeticion) {
            String descripcion = "🔄 Repetición " + repeticion.numeroRepeticiones + " veces";
            return new PatronDetectado(true, repeticion.numeroRepeticiones, false, descripcion, repeticion.grupoBase);
        }

        // Detectar ida y vuelta
        boolean tieneIdeaYVuelta = detectarIdeaYVuelta(valores);

        String descripcion;
        if (tieneIdeaYVuelta) {
            descripcion = "🔄 Ida y vuelta (manipulación completa)";
        } else {
            descripcion = "➡️ Ejecución única (movimiento directo)";
        }

        return new PatronDetectado(false, 1, tieneIdeaYVuelta, descripcion, valores);
    }

    private static class RepeticionInfo {

        boolean esRepeticion;
        int numeroRepeticiones;
        List<Integer> grupoBase;

        RepeticionInfo(boolean esRepeticion, int numeroRepeticiones, List<Integer> grupoBase) {
            this.esRepeticion = esRepeticion;
            this.numeroRepeticiones = numeroRepeticiones;
            this.grupoBase = grupoBase;
        }
    }

    private static RepeticionInfo detectarRepeticionCompleta(List<Integer> valores) {
        if (valores.size() < 16) {
            return new RepeticionInfo(false, 1, valores);
        }

        // Verificar repeticiones de 16 valores (8 pares)
        if (valores.size() == 32 && validarRepeticion(valores, 16, 2)) {
            return new RepeticionInfo(true, 2, valores.subList(0, 16));
        }

        if (valores.size() == 48 && validarRepeticion(valores, 16, 3)) {
            return new RepeticionInfo(true, 3, valores.subList(0, 16));
        }

        return new RepeticionInfo(false, 1, valores);
    }

    private static boolean validarRepeticion(List<Integer> valores, int grupoSize, int numGrupos) {
        for (int grupo = 1; grupo < numGrupos; grupo++) {
            for (int i = 0; i < grupoSize; i++) {
                if (!valores.get(i).equals(valores.get(grupo * grupoSize + i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean detectarIdeaYVuelta(List<Integer> valores) {
        int cerosEncontrados = 0;
        for (int i = 0; i < valores.size(); i += 2) {
            if (valores.get(i) == 0) {
                cerosEncontrados++;
            }
        }
        return cerosEncontrados >= 2; // Al menos 2 movimientos a 0°
    }

    private static String generarSecuenciaSegunPatron(List<Integer> valores, PatronDetectado patron) {
        StringBuilder secuencia = new StringBuilder();

        if (patron.esRepeticion) {
            secuencia.append("    -- ").append(patron.descripcion).append("\n");
            secuencia.append("    for repeticion = 1, ").append(patron.numeroRepeticiones).append(" do\n");
            secuencia.append("        print('🔄 === Ciclo ' .. repeticion .. ' de ").append(patron.numeroRepeticiones).append(" ===')\n");
            secuencia.append("        \n");
            secuencia.append(generarMovimientosArticulaciones(patron.grupoRepeticion, "        "));
            secuencia.append("        \n");
            secuencia.append("        if repeticion < ").append(patron.numeroRepeticiones).append(" then\n");
            secuencia.append("            sim.wait(1.0)\n");
            secuencia.append("        end\n");
            secuencia.append("    end\n");
        } else {
            secuencia.append("    -- ").append(patron.descripcion).append("\n");
            secuencia.append("    print('🎯 Iniciando secuencia de manipulación')\n");
            secuencia.append("    \n");
            secuencia.append(generarMovimientosArticulaciones(valores, "    "));
        }

        return secuencia.toString();
    }

    private static String generarMovimientosArticulaciones(List<Integer> valores, String indent) {
        StringBuilder movimientos = new StringBuilder();

        // Mapeo CORREGIDO: Base=1, Brazo=3, Hombro=2, Garra=7 (índices Lua)
        String[] articulaciones = {"Base", "Brazo", "Hombro", "Garra"};
        int[] indices = {1, 3, 2, 7}; // Índices corregidos
        String[] acciones = {"🔧 Orientación", "🦾 Extensión", "📐 Elevación", "🤏 Manipulación"};

        int paso = 1;
        for (int i = 0; i < valores.size() - 1; i += 2) {
            int grados = valores.get(i);
            int velocidad = valores.get(i + 1);

            // Validar valores
            if (velocidad <= 0) {
                velocidad = 30; // Velocidad por defecto
            }

            // Validar grados según articulación
            int articulacionIndex = (i / 2) % 4;
            grados = validarRangoArticulacion(articulaciones[articulacionIndex], grados);

            String nombreArticulacion = articulaciones[articulacionIndex];
            int indiceArticulacion = indices[articulacionIndex];
            String accion = acciones[articulacionIndex];

            movimientos.append(indent).append("-- Paso ").append(paso++).append(": ").append(accion).append("\n");

            String tipoMovimiento = determinarTipoMovimiento(nombreArticulacion, grados);
            String descripcion = nombreArticulacion.toUpperCase() + " " + tipoMovimiento;

            movimientos.append(indent).append("moverArticulacion(jointHandles, ").append(indiceArticulacion)
                    .append(", ").append(grados).append(", ").append(velocidad).append(", '")
                    .append(descripcion).append("')\n");
            movimientos.append(indent).append("\n");
        }

        return movimientos.toString();
    }

    private static int validarRangoArticulacion(String articulacion, int grados) {
        return switch (articulacion) {
            case "Base" ->
                Math.max(0, Math.min(360, grados));
            case "Hombro", "Brazo" ->
                Math.max(0, Math.min(180, grados));
            case "Garra" ->
                Math.max(0, Math.min(90, grados));
            default ->
                grados;
        };
    }

    private static String determinarTipoMovimiento(String articulacion, int grados) {
        if (grados == 0) {
            return "INICIAL";
        }

        return switch (articulacion) {
            case "Base" ->
                grados > 180 ? "MÁXIMO" : "MEDIO";
            case "Hombro", "Brazo" ->
                grados > 90 ? "MÁXIMO" : "MEDIO";
            case "Garra" ->
                grados > 45 ? "CERRAR" : "ABRIR";
            default ->
                "MEDIO";
        };
    }

    public static void guardarCodigoLua(String directorio, String nombreArchivo, String contenidoLua) {
        try {
            java.io.File dir = new java.io.File(directorio);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String rutaCompleta = directorio + "\\" + nombreArchivo + ".lua";

            try (FileWriter writer = new FileWriter(rutaCompleta)) {
                writer.write(contenidoLua);
                System.out.println("✓ Código Lua guardado: " + rutaCompleta);
            }
        } catch (IOException e) {
            System.err.println("✗ Error al guardar: " + e.getMessage());
        }
    }
}
