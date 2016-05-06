package com.tkotko.invescan;


import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ERPXmlParser {
    private static final String ns = null;

    // We don't use namespaces

    public List<Entry> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readResponse(parser);
        } finally {
            in.close();
        }
    }

    private List<Entry> readResponse(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Entry> entries = new ArrayList<Entry>();

        parser.require(XmlPullParser.START_TAG, ns, "Response");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("ResponseContent")) {
                parser.require(XmlPullParser.START_TAG, ns, "ResponseContent");
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    name = parser.getName();
                    if (name.equals("Document")) {
                        parser.require(XmlPullParser.START_TAG, ns, "Document");
                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() != XmlPullParser.START_TAG) {
                                continue;
                            }
                            name = parser.getName();
                            if (name.equals("RecordSet")) {
                                parser.require(XmlPullParser.START_TAG, ns, "RecordSet");
                                while (parser.next() != XmlPullParser.END_TAG) {
                                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                                        continue;
                                    }
                                    name = parser.getName();
                                    if (name.equals("Master")) {
                                        parser.require(XmlPullParser.START_TAG, ns, "Master");
                                        while (parser.next() != XmlPullParser.END_TAG) {
                                            if (parser.getEventType() != XmlPullParser.START_TAG) {
                                                continue;
                                            }
                                            name = parser.getName();
                                            if (name.equals("Record")) {
                                                entries.add(readEntry(parser));
                                            } else {
                                                skip(parser);
                                            }
                                        }
                                    } else {
                                        skip(parser);
                                    }
                                }
                            } else {
                                skip(parser);
                            }
                        }
                    } else {
                        skip(parser);
                    }
                }
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // This class represents a single entry (post) in the XML feed.
    // It includes the data members "title," "link," and "summary."
    public static class Entry {
        public final String item_id;
        public final String item_name;
        public final String item_spec;
        public final String group_id;
        public final String source_code;

        private Entry(String item_id, String item_name,String item_spec,String group_id, String source_code) {
            this.item_id = item_id;
            this.item_name = item_name;
            this.item_spec = item_spec;
            this.group_id = group_id;
            this.source_code = source_code;
        }
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Record");
        String item_id = null;
        String item_name = null;
        String item_spec = null;
        String group_id = null;
        String source_code = null;
        String parValue = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("Field")) {
                if(parValue == null){
                    parValue = readField(parser);
                }else{
                    parValue = parValue + ";;" + readField(parser);
                }

            } else {
                skip(parser);
            }
        }

        String[] spValues = parValue.split(";;");
        //item_id = spValues[0];
        //item_name = spValues[1];
        //item_spec = spValues[2];
        item_id = spValues[1];
        item_name = spValues[7];
        item_spec = spValues[25];
        group_id = "xxx";
        source_code = "xxx";

        return new Entry(item_id,item_name,item_spec,group_id,source_code);
    }

    private String readField(XmlPullParser parser) throws IOException, XmlPullParserException {
        String parValue = "";
        parser.require(XmlPullParser.START_TAG, ns, "Field");
        String tag = parser.getName();
        String FieldName = parser.getAttributeValue(null, "name");
        if (tag.equals("Field")) {
            //if (FieldName.equals("ima01")) {
            parValue = parser.getAttributeValue(null, "value");
                parser.nextTag();
            //}else {
            //    skip(parser);
            //}
        }
        parser.require(XmlPullParser.END_TAG, ns, "Field");
        return parValue;
    }

    // Processes title tags in the feed.
    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    // Processes link tags in the feed.
    private String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        String link = "";
        parser.require(XmlPullParser.START_TAG, ns, "link");
        String tag = parser.getName();
        String relType = parser.getAttributeValue(null, "rel");
        if (tag.equals("link")) {
            if (relType.equals("alternate")) {
                link = parser.getAttributeValue(null, "href");
                parser.nextTag();
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "link");
        return link;
    }

    // Processes summary tags in the feed.
    private String readSummary(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "summary");
        String summary = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "summary");
        return summary;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
