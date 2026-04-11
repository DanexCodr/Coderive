package cod.ir;

import cod.ast.node.Type;
import cod.ptac.Artifact;
import cod.ptac.Flag;
import cod.ptac.Function;
import cod.ptac.Instruction;
import cod.ptac.Operand;
import cod.ptac.OperandKind;
import cod.ptac.Opcode;
import cod.ptac.Unit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class IRArtifactCodec {
    private static final int ARTIFACT_SCHEMA_VERSION = 1;

    private IRArtifactCodec() {}

    static void writeArtifact(DataOutput out, Artifact artifact) throws IOException {
        IRCodec.writeHeader(out);
        IRCodec.writeValue(out, encodeArtifact(artifact), 0);
    }

    static Artifact readArtifact(DataInput in) throws IOException {
        IRCodec.readHeader(in);
        Object value = IRCodec.readValue(in, 0);
        if (!(value instanceof Map)) {
            throw new IOException("IR root is not an artifact map");
        }
        return decodeArtifact(castMap(value, "artifact"));
    }

    private static Map<String, Object> encodeArtifact(Artifact artifact) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("schemaVersion", Integer.valueOf(ARTIFACT_SCHEMA_VERSION));
        out.put("version", Integer.valueOf(artifact.version));
        out.put("unitName", artifact.unitName);
        out.put("className", artifact.className);
        out.put("typeSnapshot", artifact.typeSnapshot);
        out.put("unit", encodeUnit(artifact.unit));
        return out;
    }

    private static Map<String, Object> encodeUnit(Unit unit) {
        if (unit == null) return null;
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("unitName", unit.unitName);
        out.put("className", unit.className);
        out.put("entryFunction", unit.entryFunction);
        out.put("functions", encodeFunctions(unit.functions));
        return out;
    }

    private static List<Object> encodeFunctions(List<Function> functions) {
        if (functions == null) return null;
        List<Object> out = new ArrayList<Object>(functions.size());
        for (Function fn : functions) {
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("name", fn == null ? null : fn.name);
            value.put("parameters", fn == null ? null : new ArrayList<String>(safeStringList(fn.parameters)));
            value.put("instructions", fn == null ? null : encodeInstructions(fn.instructions));
            value.put("lambdaBlock", Boolean.valueOf(fn != null && fn.lambdaBlock));
            value.put("closureLevel", Integer.valueOf(fn == null ? 0 : fn.closureLevel));
            out.add(value);
        }
        return out;
    }

    private static List<Object> encodeInstructions(List<Instruction> instructions) {
        if (instructions == null) return null;
        List<Object> out = new ArrayList<Object>(instructions.size());
        for (Instruction instruction : instructions) {
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("opcode", instruction == null || instruction.opcode == null ? null : instruction.opcode.name());
            value.put("dest", instruction == null ? null : instruction.dest);
            value.put("operands", instruction == null ? null : encodeOperands(instruction.operands));
            value.put("flags", instruction == null ? null : encodeFlags(instruction.flags));
            out.add(value);
        }
        return out;
    }

    private static List<Object> encodeOperands(List<Operand> operands) {
        if (operands == null) return null;
        List<Object> out = new ArrayList<Object>(operands.size());
        for (Operand operand : operands) {
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("kind", operand == null || operand.kind == null ? null : operand.kind.name());
            value.put("value", operand == null ? null : operand.value);
            out.add(value);
        }
        return out;
    }

    private static List<Object> encodeFlags(EnumSet<Flag> flags) {
        if (flags == null) return null;
        List<Object> out = new ArrayList<Object>(flags.size());
        for (Flag flag : flags) {
            out.add(flag == null ? null : flag.name());
        }
        return out;
    }

    private static Artifact decodeArtifact(Map<String, Object> map) throws IOException {
        int schemaVersion = readInt(map.get("schemaVersion"), "schemaVersion");
        if (schemaVersion != ARTIFACT_SCHEMA_VERSION) {
            throw new IOException("Unsupported artifact schema version: " + schemaVersion);
        }

        Artifact artifact = new Artifact();
        artifact.version = readInt(map.get("version"), "version");
        artifact.unitName = asString(map.get("unitName"), "unitName");
        artifact.className = asString(map.get("className"), "className");
        artifact.typeSnapshot = asType(map.get("typeSnapshot"));
        artifact.unit = decodeUnit(map.get("unit"));
        return artifact;
    }

    private static Unit decodeUnit(Object value) throws IOException {
        if (value == null) return null;
        Map<String, Object> map = castMap(value, "unit");
        Unit unit = new Unit();
        unit.unitName = asString(map.get("unitName"), "unit.unitName");
        unit.className = asString(map.get("className"), "unit.className");
        unit.entryFunction = asString(map.get("entryFunction"), "unit.entryFunction");
        unit.functions = decodeFunctions(map.get("functions"));
        return unit;
    }

    private static List<Function> decodeFunctions(Object value) throws IOException {
        if (value == null) return new ArrayList<Function>();
        List<Object> list = castList(value, "unit.functions");
        List<Function> out = new ArrayList<Function>(list.size());
        for (Object item : list) {
            Map<String, Object> map = castMap(item, "function");
            Function function = new Function();
            function.name = asString(map.get("name"), "function.name");
            function.parameters = asStringList(map.get("parameters"), "function.parameters");
            function.instructions = decodeInstructions(map.get("instructions"));
            function.lambdaBlock = readBoolean(map.get("lambdaBlock"), "function.lambdaBlock");
            function.closureLevel = readInt(map.get("closureLevel"), "function.closureLevel");
            out.add(function);
        }
        return out;
    }

    private static List<Instruction> decodeInstructions(Object value) throws IOException {
        if (value == null) return new ArrayList<Instruction>();
        List<Object> list = castList(value, "function.instructions");
        List<Instruction> out = new ArrayList<Instruction>(list.size());
        for (Object item : list) {
            Map<String, Object> map = castMap(item, "instruction");
            String opcodeName = asString(map.get("opcode"), "instruction.opcode");
            Opcode opcode = parseEnum(Opcode.class, opcodeName, "instruction.opcode");
            String dest = asString(map.get("dest"), "instruction.dest");
            List<Operand> operands = decodeOperands(map.get("operands"));
            EnumSet<Flag> flags = decodeFlags(map.get("flags"));
            out.add(new Instruction(opcode, dest, operands, flags));
        }
        return out;
    }

    private static List<Operand> decodeOperands(Object value) throws IOException {
        if (value == null) return new ArrayList<Operand>();
        List<Object> list = castList(value, "instruction.operands");
        List<Operand> out = new ArrayList<Operand>(list.size());
        for (Object item : list) {
            Map<String, Object> map = castMap(item, "operand");
            String kindName = asString(map.get("kind"), "operand.kind");
            OperandKind kind = parseEnum(OperandKind.class, kindName, "operand.kind");
            Object operandValue = map.get("value");
            out.add(createOperand(kind, operandValue));
        }
        return out;
    }

    private static EnumSet<Flag> decodeFlags(Object value) throws IOException {
        EnumSet<Flag> out = EnumSet.noneOf(Flag.class);
        if (value == null) return out;
        List<Object> list = castList(value, "instruction.flags");
        for (Object item : list) {
            String flagName = asString(item, "instruction.flag");
            Flag flag = parseEnum(Flag.class, flagName, "instruction.flag");
            out.add(flag);
        }
        return out;
    }

    private static Operand createOperand(OperandKind kind, Object value) throws IOException {
        if (kind == null) {
            throw new IOException("operand.kind is null");
        }
        switch (kind) {
            case REGISTER:
                return Operand.register(asString(value, "operand.value"));
            case IMMEDIATE:
                return Operand.immediate(value);
            case LABEL:
                return Operand.label(asString(value, "operand.value"));
            case FUNCTION:
                return Operand.function(asString(value, "operand.value"));
            case SLOT:
                return Operand.slot(asString(value, "operand.value"));
            case IDENTIFIER:
                return Operand.identifier(asString(value, "operand.value"));
            default:
                throw new IOException("Unsupported operand kind: " + kind);
        }
    }

    private static Type asType(Object value) throws IOException {
        if (value == null) return null;
        if (value instanceof Type) return (Type) value;
        throw new IOException("Expected Type, got: " + value.getClass().getName());
    }

    private static String asString(Object value, String fieldName) throws IOException {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        throw new IOException("Expected String for " + fieldName + ", got: " + value.getClass().getName());
    }

    private static int readInt(Object value, String fieldName) throws IOException {
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        throw new IOException("Expected Integer for " + fieldName + ", got: "
            + (value == null ? "null" : value.getClass().getName()));
    }

    private static boolean readBoolean(Object value, String fieldName) throws IOException {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        throw new IOException("Expected Boolean for " + fieldName + ", got: "
            + (value == null ? "null" : value.getClass().getName()));
    }

    private static List<String> asStringList(Object value, String fieldName) throws IOException {
        if (value == null) return new ArrayList<String>();
        List<Object> list = castList(value, fieldName);
        List<String> out = new ArrayList<String>(list.size());
        for (Object item : list) {
            out.add(asString(item, fieldName + " item"));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value, String fieldName) throws IOException {
        if (!(value instanceof Map)) {
            throw new IOException("Expected Map for " + fieldName + ", got: "
                + (value == null ? "null" : value.getClass().getName()));
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object value, String fieldName) throws IOException {
        if (!(value instanceof List)) {
            throw new IOException("Expected List for " + fieldName + ", got: "
                + (value == null ? "null" : value.getClass().getName()));
        }
        return (List<Object>) value;
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String name, String fieldName) throws IOException {
        if (name == null) {
            throw new IOException(fieldName + " is null");
        }
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid enum constant '" + name + "' for " + fieldName, e);
        }
    }

    private static List<String> safeStringList(List<String> values) {
        return values == null ? new ArrayList<String>() : values;
    }
}
