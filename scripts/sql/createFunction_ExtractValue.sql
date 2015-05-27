
delimiter //

CREATE FUNCTION `EXTRACTVALUE_ALL`(p_xml MEDIUMTEXT, p_xpathExpr TEXT, p_delimiter TEXT) RETURNS text CHARSET latin1
    DETERMINISTIC
BEGIN
DECLARE total_elements INT;
DECLARE xpath_expression_count, xpath_expression_index  TEXT;
DECLARE single_tag, result  TEXT;

# calc TOTAL elements
SET xpath_expression_count = CONCAT('count(', p_xpathExpr, ')');
SELECT EXTRACTVALUE(p_xml, xpath_expression_count) INTO total_elements;

# run over elements (create long text)
SET result = '';
SET @i = 1;
WHILE @i <= total_elements DO
  SET xpath_expression_index = CONCAT(p_xpathExpr, '[', @i, ']');
  SET single_tag = EXTRACTVALUE(p_xml, xpath_expression_index);
  SET result = IF(result='', single_tag, CONCAT(result, p_delimiter, single_tag));
  SET @i = @i + 1;
END WHILE;

# return total result
RETURN result;
END;//

delimiter ;