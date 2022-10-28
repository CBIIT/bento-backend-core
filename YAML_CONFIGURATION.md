# YAML Configuration Guide
This is the user documentation to build a codeless search API written in YAML retrieving data from Open-search.

## Introduction
The codeless Yaml configuration is a tool to implement Open-search APIs without writing Java code in Bento Backend.

## Required Configuration Files
The following file must be added for the Yaml configuration.
1. Open-search Yaml scheme in Data-Loader
2. Search API YAML Script in Bento-Backend

| File Type | Description                                    |
|-----------|------------------------------------------------|
| Single    | Request a single query                         |
| Facet     | Request multiple queries at a time             |
| Global    | Request a pre-determined global search queries |

   
## 1. Open-search Yaml scheme in Data-Loader
This is to explain for an Open-search data schema exporting Neo4j data into Open-search

### Open-Search Schema Description
| Name         | Description                                                                                                                                                                                                                                                                                                                                     |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| index_name   | Declare a name of index in Open-search                                                                                                                                                                                                                                                                                                          |
| type         | Declare a type of data-source where to import                                                                                                                                                                                                                                                                                                   |
| mapping      | Declare a list of fields and property types <br/> <table>  <tbody>  <tr>  <td>Name</td> <td>Description</td></tr> <tr> <td>field_name</td>  <td>Declare a name of field</td></tr> <tr> <td>properties</td>  <td>Declare a data type of the field </br> ex) keyword, integer, long, double, object, text, nested   </td></tr> </tbody>  </table> |
| cypher_query | Neo4j graph query that lets you retrieve data                                                                                                                                                                                                                                                                                                   |

## 2. Search API YAML Script in Bento-Backend
  - index: Declare Open-search Index Name(s)
  - name: Declare a GraphQL Query Name
  - filter: Declare a filter type in GraphQl scheme. See Table FY-1.0
  - result: Declare Desired Return Type See Table RT-1.0
  

## Filter Type Table - FY-1.0
| Filter Type     | Description                                                                                                                                                                                                                                                                                                                                  |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| default         | Search in the selectedField through Open-search index <br/> <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>selectedField</td>  <td>O</td>  <td>-</td></tr> </tbody>  </table>                                                                                                                  |
| aggregation     | Search to group the summary of documents into buckets <br/> <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>selectedField</td>  <td>O</td>  <td>-</td></tr> <tr> <td>ignoreSelectedField</td>  <td>X</td>  <td>-</td></tr></tbody>  </table>                                                    |
| pagination      | Search with pagination params including size, offset, and order-by <br/> <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>defaultSortField</td>  <td>X</td>  <td>-</td></tr> <tr> <td>alternativeSortField</td>  <td>X</td>  <td>-</td></tr> </tbody>  </table>                                  |
| range           | Search within numerical boundary <br/> <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>selectedField</td>  <td>O</td>  <td>-</td></tr></tbody>  </table>                                                                                                                                        |
| sub_aggregation | In addition to aggregation, grouping the summary of each document per bucket <br/>  <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>selectedField</td>  <td>O</td>  <td>-</td></tr><tr> <td>subAggSelectedField</td>  <td>O</td>  <td>-</td></tr> </tbody>  </table>                            |
| global          | Search based on a precise value<br/> <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>defaultSortField</td>  <td>O</td>  <td>-</td></tr> <tr> <td>query</td>  <td>O</td>  <td>query search desc</td></tr> <tr> <td>typedSearch</td>  <td>X</td>  <td>type search des</td></tr></tbody>  </table> |
| nested          | Searches it in a nested field objects <br/> <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>defaultSortField</td>  <td>O</td>  <td>-</td></tr> <tr> <td>nestedParameters</td>  <td>X</td>  <td>-</td></tr></tbody>  </table> <br/>                                                              |
| sum             | Sum up numerical values from the aggregated search. <br/> <table>  <tbody>  <tr>  <td>Name</td>  <td>Required</td>  <td>Description</td></tr> <tr> <td>selectedField</td>  <td>O</td>  <td>-</td></tr> </tbody>  </table>                                                                                                                    |

## Global Highlighter
pre-requisite: In order to use highlight, a filter type must be stored as global

| Name         | Required | Description                                                    |
|--------------|----------|----------------------------------------------------------------|
| fields       | O        | Array of fields to highlight                                   |
| fragmentSize | X        | The size of the highlighted fragment. Default by 1             |
| preTag       | X        | Use html tag to wrap before the highlighted text. Default by $ |
| postTag      | X        | Use html tag to wrap after the highlighted text. Default by $  |

## Query Configuration Result Type Table - RT-1.0
| Result Type         | Optional Method                                       | Example                                                                  |
|---------------------|-------------------------------------------------------|--------------------------------------------------------------------------|
| object_array        | -                                                     | [object, object, object...]                                              |
| group_count         | -                                                     | {boy: 10, girl:20...}                                                    |
| str_array           | -                                                     | [ boy, girl...]                                                          |
| int                 | count_bucket_keys<br/> nested_count. see Table OM-1.0 | 1, 2, 3 ...                                                              |
| float               | sum. see Table OM-1.0                                 | 1.0, 2.0, 3.0...                                                         |
| global_about        | -                                                     | {type: '', page: '', title: '', text: ''}                                |
| global              | -                                                     | {boy: object, girl: object}                                              |
| global_multi_models | -                                                     | {boy: object, girl:object}                                               |
| range               | -                                                     | {lowerBound: 0.00, upperBound: 0.00, subjects: XXX}                      |
| arm_program         | -                                                     | {program: 0, caseSize: 0, children: [{arm: 0, caseSize: 0, size: 0}...]} |
| empty               | -                                                     | 0                                                                        |


#### Optional Method Table - OM-1.0
| Optional Method   | Description                                                            |
|-------------------|------------------------------------------------------------------------|
| count_bucket_keys | Return an integer for the result of aggregation filter                 |
| nested_count      | Return an integer for the result of nested aggregation filter          |
| sum               | Return an float; sum of all aggregated data for sum aggregation filter |


## Query Configuration Filter & Result Pair Rule
| Query Type | Description                                                                                 |
|------------|---------------------------------------------------------------------------------------------|
| term       | Searches based on a precise value like an userid or a price<br/> Optional: integer, boolean |
| match      | Searched text, number or boolean value after text scoring analysis                          |
| wildcard   | Searches items without knowing the exact words                                              |

## Query Pairing
A filter type must pair with a result type

| Query Type      | Result Type                                  |
|-----------------|----------------------------------------------|
| default         | object_array, str_array                      |
| pagination      | object_array                                 |
| sub_aggregation | arm_program                                  |
| aggregation     | aggregation, int(count_bucket_keys)          |
| range           | range                                        |
| global          | global, global_multi_models                  |
| nested          | int(nested_count), nested_list, nested_total |
| sum             | float(sum)                                   |