# Codec for HTML Fix structure

## Introduction

**th2-codec-hand-html** is responsible for receiving *RawMessages* via RabbitMQ, which contains Fix structure
provided in a HTML table format, parsing and processing it, generating *Message* and then sending it via RabbitMQ.

##### Caveats
Hierarchy described below is an arbitrary concept for a HTML table, enough information should be supplemented with a configuration, so that a multi-layer map like structure can be constructed from the flat HTML table.

## Quick Start
#### Configuration
Config which should be provided is defined as *FixHtmlProcessorConfiguration* and looks like this:
```
{
  "messageTypeElClassName" : "table-header",
  "contentKey" : "Text out",
  "hierarchyStart" : 10,
  "hierarchyStep" : 10,
  "hierarchyAttribute" : "style",
  "hierarchyIndicatorPrefix" : "padding-left: ",
  "hierarchyIndicatorSuffix" : "px"
}
```
**messageTypeElClassName** - HTML class name for an element which contains Fix message type

**contentKey** - Name of a field from which Fix table should be extracted

**hierarchyStart** - Numeric value for initial depth of hierarchy

**hierarchyStep** - Numeric value for a depth parameter to indicate stepping in/stepping out from current hierarchy layer

**hierarchyAttribute** - An HTML tag attribute from which hierarchy indicators should be extracted

**hierarchyIndicatorPrefix** - Prefix which is being searched inside *hierarchyAttribute* to get the leftmost index for hierarchy indicator

**hierarchyIndicatorSuffix** - Suffix which is being searched after *hierarchyIndicatorPrefix* to get the rightmost index for hierarchy indicator
