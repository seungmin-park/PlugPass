package com.plugpass.ingestion.client;

import com.plugpass.ingestion.dto.response.PublicDataItem;
import com.plugpass.ingestion.dto.response.PublicDataResponse;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.IntStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

final class PublicDataXmlParser {
    PublicDataResponse parse(byte[] xml, int requestedPageCapacity) {
        try {
            Element response = parseDocument(xml).getDocumentElement();
            if (!"response".equals(response.getTagName())) {
                throw contractFailure();
            }
            Element header = requiredChild(response, "header");
            requireProviderSuccess(requiredText(header, "resultCode"));
            int pageNumber = Integer.parseInt(requiredText(header, "pageNo").strip());
            int reportedRowCount = Integer.parseInt(requiredText(header, "numOfRows").strip());
            long totalCount = Long.parseLong(requiredText(header, "totalCount").strip());
            if (pageNumber < 1 || reportedRowCount < 0 || reportedRowCount > 9999 || totalCount < 0) {
                throw contractFailure();
            }
            Element items = requiredChild(requiredChild(response, "body"), "items");
            List<PublicDataItem> result = children(items, "item").stream().map(this::readItem).toList();
            long firstIndex = ((long) pageNumber - 1) * requestedPageCapacity;
            if (result.size() > reportedRowCount || result.size() > requestedPageCapacity || (result.isEmpty() && firstIndex < totalCount)
                    || (!result.isEmpty() && firstIndex + result.size() > totalCount)) {
                throw contractFailure();
            }
            return new PublicDataResponse(pageNumber, reportedRowCount, totalCount, result);
        } catch (IllegalArgumentException exception) {
            throw contractFailure();
        }
    }
    private Document parseDocument(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }
                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw contractFailure();
        }
    }
    private PublicDataItem readItem(Element item) {
        return new PublicDataItem(requiredText(item,"busiId"), requiredText(item,"statId"),
                requiredText(item,"chgerId"), requiredText(item,"statNm"), requiredText(item,"lat"), requiredText(item,"lng"),
                text(item,"stat"), text(item,"chgerType"), text(item,"useTime"), text(item,"limitYn"), text(item,"limitDetail"),
                text(item,"note"), text(item,"statUpdDt"), text(item,"lastTsdt"), text(item,"lastTedt"), text(item,"nowTsdt"),
                text(item,"delYn"), text(item,"delDetail"));
    }
    private List<Element> children(Element parent, String name) {
        NodeList nodes = parent.getChildNodes();
        return IntStream.range(0, nodes.getLength()).mapToObj(nodes::item)
                .filter(Element.class::isInstance).map(Element.class::cast)
                .filter(element -> name.equals(element.getTagName())).toList();
    }
    private Element requiredChild(Element parent, String name) {
        return children(parent, name).stream().findFirst().orElseThrow(this::contractFailure);
    }
    private String text(Element parent, String name) {
        return children(parent, name).stream().findFirst().map(Element::getTextContent).orElse(null);
    }
    private String requiredText(Element parent, String name) {
        String value = text(parent, name);
        if (value == null || value.isBlank()) {
            throw contractFailure();
        }
        return value;
    }
    private void requireProviderSuccess(String code) {
        if ("00".equals(code)) {
            return;
        }
        PublicDataFailure failure = switch (code) {
            case "20", "30", "31" -> PublicDataFailure.AUTHENTICATION;
            case "22", "23" -> PublicDataFailure.RATE_LIMIT;
            case "05" -> PublicDataFailure.TIMEOUT;
            case "01" -> PublicDataFailure.SERVER;
            default -> PublicDataFailure.CONTRACT;
        };
        throw new PublicDataException(failure);
    }
    private PublicDataException contractFailure() {
        return new PublicDataException(PublicDataFailure.CONTRACT);
    }
}
