"""
Tool to populate a Word document template with data from a JSON string.
Uses docxtpl library for template population.
"""

from ibm_watsonx_orchestrate.agent_builder.tools import tool
from docxtpl import DocxTemplate
import io
import json
from typing import Dict, Any


@tool
def populate_word_template(json_data: str, template_bytes: bytes) -> bytes:
    """
    Populate a Word document template with data from a JSON string.
    
    This tool takes a JSON string containing key-value pairs and a Word document
    template (as bytes), then replaces template placeholders with the provided data.
    The template should use Jinja2 syntax for placeholders (e.g., {{ variable_name }}).
    
    Args:
        json_data: A JSON string containing the data to populate the template.
                   Example: '{"name": "John Doe", "position": "Software Engineer"}'
        template_bytes: The Word document template as bytes. The template should
                       contain Jinja2-style placeholders like {{ name }}, {{ position }}, etc.
    
    Returns:
        bytes: The populated Word document as bytes.
    
    Raises:
        ValueError: If the JSON string is invalid or template processing fails.
        Exception: If there's an error reading the template or generating the document.
    
    Example:
        >>> json_str = '{"employee_name": "Jane Smith", "department": "Engineering"}'
        >>> template = open("template.docx", "rb").read()
        >>> result = populate_word_template(json_str, template)
        >>> with open("output.docx", "wb") as f:
        ...     f.write(result)
    """
    try:
        # Parse the JSON string
        data = json.loads(json_data)

        # Process nested JSON strings in the data (from KVP extraction)
        data_dict = _process_nested_json(data)
        
        # Load the template from bytes
        template_stream = io.BytesIO(template_bytes)
        doc = DocxTemplate(template_stream)
        
        # Render the template with the data
        doc.render(data_dict)
        
        # Save the populated document to bytes
        output_stream = io.BytesIO()
        doc.save(output_stream)
        output_stream.seek(0)
        
        return output_stream.read()
        
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON string: {str(e)}")
    except Exception as e:
        raise Exception(f"Error populating Word template: {str(e)}")


def _process_nested_json(data: Dict[str, Any]) -> Dict[str, Any]:
    """
    Process nested JSON strings in the data dictionary.
    
    Some fields from KVP extraction may contain JSON strings that need to be
    parsed into actual arrays or objects for proper template rendering.
    
    Args:
        data: Dictionary that may contain JSON strings as values
        
    Returns:
        Dictionary with JSON strings parsed into proper structures
    """
    processed_data = {}
    
    for key, value in data.items():
        if isinstance(value, str):
            # Try to parse as JSON if it looks like JSON
            if value.strip().startswith(('[', '{')):
                try:
                    processed_data[key] = json.loads(value)
                except json.JSONDecodeError:
                    # If parsing fails, keep as string
                    processed_data[key] = value
            else:
                processed_data[key] = value
        elif isinstance(value, dict):
            # Recursively process nested dictionaries
            processed_data[key] = _process_nested_json(value)
        elif isinstance(value, list):
            # Process each item in the list
            processed_data[key] = [
                _process_nested_json(item) if isinstance(item, dict) else item
                for item in value
            ]
        else:
            processed_data[key] = value
    
    return processed_data


# Made with Bob
