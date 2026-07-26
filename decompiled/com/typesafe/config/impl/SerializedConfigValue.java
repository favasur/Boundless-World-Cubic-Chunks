package com.typesafe.config.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class SerializedConfigValue extends AbstractConfigValue implements Externalizable {
   private static final long serialVersionUID = 1L;
   private ConfigValue value;
   private boolean wasConfig;

   public SerializedConfigValue() {
      super(null);
   }

   SerializedConfigValue(ConfigValue value) {
      this();
      this.value = value;
      this.wasConfig = false;
   }

   SerializedConfigValue(Config conf) {
      this(conf.root());
      this.wasConfig = true;
   }

   private Object readResolve() throws ObjectStreamException {
      return this.wasConfig ? ((ConfigObject)this.value).toConfig() : this.value;
   }

   private static void writeOriginField(DataOutput out, SerializedConfigValue.SerializedField code, Object v) throws IOException {
      switch (code) {
         case ORIGIN_DESCRIPTION:
            out.writeUTF((String)v);
            break;
         case ORIGIN_LINE_NUMBER:
            out.writeInt((Integer)v);
            break;
         case ORIGIN_END_LINE_NUMBER:
            out.writeInt((Integer)v);
            break;
         case ORIGIN_TYPE:
            out.writeByte((Integer)v);
            break;
         case ORIGIN_URL:
            out.writeUTF((String)v);
            break;
         case ORIGIN_COMMENTS:
            List<String> list = (List<String>)v;
            int size = list.size();
            out.writeInt(size);

            for (String s : list) {
               out.writeUTF(s);
            }
         case ORIGIN_NULL_URL:
         case ORIGIN_NULL_COMMENTS:
            break;
         default:
            throw new IOException("Unhandled field from origin: " + code);
      }
   }

   static void writeOrigin(DataOutput out, SimpleConfigOrigin origin, SimpleConfigOrigin baseOrigin) throws IOException {
      Map<SerializedConfigValue.SerializedField, Object> m;
      if (origin != null) {
         m = origin.toFieldsDelta(baseOrigin);
      } else {
         m = Collections.emptyMap();
      }

      for (Entry<SerializedConfigValue.SerializedField, Object> e : m.entrySet()) {
         SerializedConfigValue.FieldOut field = new SerializedConfigValue.FieldOut(e.getKey());
         Object v = e.getValue();
         writeOriginField(field.data, field.code, v);
         writeField(out, field);
      }

      writeEndMarker(out);
   }

   static SimpleConfigOrigin readOrigin(DataInput in, SimpleConfigOrigin baseOrigin) throws IOException {
      Map<SerializedConfigValue.SerializedField, Object> m = new EnumMap<>(SerializedConfigValue.SerializedField.class);

      while (true) {
         Object v = null;
         SerializedConfigValue.SerializedField field = readCode(in);
         switch (field) {
            case ORIGIN_DESCRIPTION:
               in.readInt();
               v = in.readUTF();
               break;
            case ORIGIN_LINE_NUMBER:
               in.readInt();
               v = in.readInt();
               break;
            case ORIGIN_END_LINE_NUMBER:
               in.readInt();
               v = in.readInt();
               break;
            case ORIGIN_TYPE:
               in.readInt();
               v = in.readUnsignedByte();
               break;
            case ORIGIN_URL:
               in.readInt();
               v = in.readUTF();
               break;
            case ORIGIN_COMMENTS:
               in.readInt();
               int size = in.readInt();
               List<String> list = new ArrayList<>(size);

               for (int i = 0; i < size; i++) {
                  list.add(in.readUTF());
               }

               v = list;
               break;
            case ORIGIN_NULL_URL:
            case ORIGIN_NULL_COMMENTS:
               in.readInt();
               v = "";
               break;
            case END_MARKER:
               return SimpleConfigOrigin.fromBase(baseOrigin, m);
            case ROOT_VALUE:
            case ROOT_WAS_CONFIG:
            case VALUE_DATA:
            case VALUE_ORIGIN:
               throw new IOException("Not expecting this field here: " + field);
            case UNKNOWN:
               skipField(in);
         }

         if (v != null) {
            m.put(field, v);
         }
      }
   }

   private static void writeValueData(DataOutput out, ConfigValue value) throws IOException {
      SerializedConfigValue.SerializedValueType st = SerializedConfigValue.SerializedValueType.forValue(value);
      out.writeByte(st.ordinal());
      switch (st) {
         case BOOLEAN:
            out.writeBoolean(((ConfigBoolean)value).unwrapped());
         case NULL:
         default:
            break;
         case INT:
            out.writeInt(((ConfigInt)value).unwrapped());
            out.writeUTF(((ConfigNumber)value).transformToString());
            break;
         case LONG:
            out.writeLong(((ConfigLong)value).unwrapped());
            out.writeUTF(((ConfigNumber)value).transformToString());
            break;
         case DOUBLE:
            out.writeDouble(((ConfigDouble)value).unwrapped());
            out.writeUTF(((ConfigNumber)value).transformToString());
            break;
         case STRING:
            out.writeUTF(((ConfigString)value).unwrapped());
            break;
         case LIST:
            ConfigList list = (ConfigList)value;
            out.writeInt(list.size());

            for (ConfigValue v : list) {
               writeValue(out, v, (SimpleConfigOrigin)list.origin());
            }
            break;
         case OBJECT:
            ConfigObject obj = (ConfigObject)value;
            out.writeInt(obj.size());

            for (Entry<String, ConfigValue> e : obj.entrySet()) {
               out.writeUTF(e.getKey());
               writeValue(out, e.getValue(), (SimpleConfigOrigin)obj.origin());
            }
      }
   }

   private static AbstractConfigValue readValueData(DataInput in, SimpleConfigOrigin origin) throws IOException {
      int stb = in.readUnsignedByte();
      SerializedConfigValue.SerializedValueType st = SerializedConfigValue.SerializedValueType.forInt(stb);
      if (st == null) {
         throw new IOException("Unknown serialized value type: " + stb);
      } else {
         switch (st) {
            case BOOLEAN:
               return new ConfigBoolean(origin, in.readBoolean());
            case NULL:
               return new ConfigNull(origin);
            case INT:
               int vi = in.readInt();
               String si = in.readUTF();
               return new ConfigInt(origin, vi, si);
            case LONG:
               long vl = in.readLong();
               String sl = in.readUTF();
               return new ConfigLong(origin, vl, sl);
            case DOUBLE:
               double vd = in.readDouble();
               String sd = in.readUTF();
               return new ConfigDouble(origin, vd, sd);
            case STRING:
               return new ConfigString(origin, in.readUTF());
            case LIST:
               int listSize = in.readInt();
               List<AbstractConfigValue> list = new ArrayList<>(listSize);

               for (int i = 0; i < listSize; i++) {
                  AbstractConfigValue v = readValue(in, origin);
                  list.add(v);
               }

               return new SimpleConfigList(origin, list);
            case OBJECT:
               int mapSize = in.readInt();
               Map<String, AbstractConfigValue> map = new HashMap<>(mapSize);

               for (int i = 0; i < mapSize; i++) {
                  String key = in.readUTF();
                  AbstractConfigValue v = readValue(in, origin);
                  map.put(key, v);
               }

               return new SimpleConfigObject(origin, map);
            default:
               throw new IOException("Unhandled serialized value type: " + st);
         }
      }
   }

   private static void writeValue(DataOutput out, ConfigValue value, SimpleConfigOrigin baseOrigin) throws IOException {
      SerializedConfigValue.FieldOut origin = new SerializedConfigValue.FieldOut(SerializedConfigValue.SerializedField.VALUE_ORIGIN);
      writeOrigin(origin.data, (SimpleConfigOrigin)value.origin(), baseOrigin);
      writeField(out, origin);
      SerializedConfigValue.FieldOut data = new SerializedConfigValue.FieldOut(SerializedConfigValue.SerializedField.VALUE_DATA);
      writeValueData(data.data, value);
      writeField(out, data);
      writeEndMarker(out);
   }

   private static AbstractConfigValue readValue(DataInput in, SimpleConfigOrigin baseOrigin) throws IOException {
      AbstractConfigValue value = null;
      SimpleConfigOrigin origin = null;

      while (true) {
         SerializedConfigValue.SerializedField code = readCode(in);
         if (code == SerializedConfigValue.SerializedField.END_MARKER) {
            if (value == null) {
               throw new IOException("No value data found in serialization of value");
            }

            return value;
         }

         if (code == SerializedConfigValue.SerializedField.VALUE_DATA) {
            if (origin == null) {
               throw new IOException("Origin must be stored before value data");
            }

            in.readInt();
            value = readValueData(in, origin);
         } else if (code == SerializedConfigValue.SerializedField.VALUE_ORIGIN) {
            in.readInt();
            origin = readOrigin(in, baseOrigin);
         } else {
            skipField(in);
         }
      }
   }

   private static void writeField(DataOutput out, SerializedConfigValue.FieldOut field) throws IOException {
      byte[] bytes = field.bytes.toByteArray();
      out.writeByte(field.code.ordinal());
      out.writeInt(bytes.length);
      out.write(bytes);
   }

   private static void writeEndMarker(DataOutput out) throws IOException {
      out.writeByte(SerializedConfigValue.SerializedField.END_MARKER.ordinal());
   }

   private static SerializedConfigValue.SerializedField readCode(DataInput in) throws IOException {
      int c = in.readUnsignedByte();
      if (c == SerializedConfigValue.SerializedField.UNKNOWN.ordinal()) {
         throw new IOException("field code " + c + " is not supposed to be on the wire");
      } else {
         return SerializedConfigValue.SerializedField.forInt(c);
      }
   }

   private static void skipField(DataInput in) throws IOException {
      int len = in.readInt();
      int skipped = in.skipBytes(len);
      if (skipped < len) {
         byte[] bytes = new byte[len - skipped];
         in.readFully(bytes);
      }
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      if (((AbstractConfigValue)this.value).resolveStatus() != ResolveStatus.RESOLVED) {
         throw new NotSerializableException("tried to serialize a value with unresolved substitutions, need to Config#resolve() first, see API docs");
      } else {
         SerializedConfigValue.FieldOut field = new SerializedConfigValue.FieldOut(SerializedConfigValue.SerializedField.ROOT_VALUE);
         writeValue(field.data, this.value, null);
         writeField(out, field);
         field = new SerializedConfigValue.FieldOut(SerializedConfigValue.SerializedField.ROOT_WAS_CONFIG);
         field.data.writeBoolean(this.wasConfig);
         writeField(out, field);
         writeEndMarker(out);
      }
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      while (true) {
         SerializedConfigValue.SerializedField code = readCode(in);
         if (code == SerializedConfigValue.SerializedField.END_MARKER) {
            return;
         }

         if (code == SerializedConfigValue.SerializedField.ROOT_VALUE) {
            in.readInt();
            this.value = readValue(in, null);
         } else if (code == SerializedConfigValue.SerializedField.ROOT_WAS_CONFIG) {
            in.readInt();
            this.wasConfig = in.readBoolean();
         } else {
            skipField(in);
         }
      }
   }

   private static ConfigException shouldNotBeUsed() {
      return new ConfigException.BugOrBroken(SerializedConfigValue.class.getName() + " should not exist outside of serialization");
   }

   @Override
   public ConfigValueType valueType() {
      throw shouldNotBeUsed();
   }

   @Override
   public Object unwrapped() {
      throw shouldNotBeUsed();
   }

   protected SerializedConfigValue newCopy(ConfigOrigin origin) {
      throw shouldNotBeUsed();
   }

   private static class FieldOut {
      final SerializedConfigValue.SerializedField code;
      final ByteArrayOutputStream bytes;
      final DataOutput data;

      FieldOut(SerializedConfigValue.SerializedField code) {
         this.code = code;
         this.bytes = new ByteArrayOutputStream();
         this.data = new DataOutputStream(this.bytes);
      }
   }

   static enum SerializedField {
      UNKNOWN,
      END_MARKER,
      ROOT_VALUE,
      ROOT_WAS_CONFIG,
      VALUE_DATA,
      VALUE_ORIGIN,
      ORIGIN_DESCRIPTION,
      ORIGIN_LINE_NUMBER,
      ORIGIN_END_LINE_NUMBER,
      ORIGIN_TYPE,
      ORIGIN_URL,
      ORIGIN_COMMENTS,
      ORIGIN_NULL_URL,
      ORIGIN_NULL_COMMENTS;

      private SerializedField() {
      }

      static SerializedConfigValue.SerializedField forInt(int b) {
         return b < values().length ? values()[b] : UNKNOWN;
      }
   }

   private static enum SerializedValueType {
      NULL(ConfigValueType.NULL),
      BOOLEAN(ConfigValueType.BOOLEAN),
      INT(ConfigValueType.NUMBER),
      LONG(ConfigValueType.NUMBER),
      DOUBLE(ConfigValueType.NUMBER),
      STRING(ConfigValueType.STRING),
      LIST(ConfigValueType.LIST),
      OBJECT(ConfigValueType.OBJECT);

      ConfigValueType configType;

      private SerializedValueType(ConfigValueType configType) {
         this.configType = configType;
      }

      static SerializedConfigValue.SerializedValueType forInt(int b) {
         return b < values().length ? values()[b] : null;
      }

      static SerializedConfigValue.SerializedValueType forValue(ConfigValue value) {
         ConfigValueType t = value.valueType();
         if (t == ConfigValueType.NUMBER) {
            if (value instanceof ConfigInt) {
               return INT;
            }

            if (value instanceof ConfigLong) {
               return LONG;
            }

            if (value instanceof ConfigDouble) {
               return DOUBLE;
            }
         } else {
            for (SerializedConfigValue.SerializedValueType st : values()) {
               if (st.configType == t) {
                  return st;
               }
            }
         }

         throw new ConfigException.BugOrBroken("don't know how to serialize " + value);
      }
   }
}
