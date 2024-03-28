import React, { useEffect, useRef, useState } from "react";

import AvatarEditor from "react-avatar-editor";
import Button from "react-bootstrap/esm/Button";
import Col from "react-bootstrap/esm/Col";
import Container from "react-bootstrap/esm/Container";
import Row from "react-bootstrap/esm/Row";
import Form from "react-bootstrap/Form";
import Image from "react-bootstrap/Image";
import imgplaceholder from "../../image-placeholder.jpeg";
import { Card } from "react-bootstrap";

function ImageEditor({inputImageB64, getEditedImageB64, edit, setEdit}) {
    const [editMode , setEditMode] = useState(edit);
    const [image, setImage] = useState(inputImageB64);
    const [scale, setScale] = useState(1);
    const editor = useRef();

    const cropImage = () => {
        const canvas = editor.current.getImageScaledToCanvas();
        setScale(1);
        getEditedImageB64(canvas.toDataURL());
        setEdit(false);
    }

    useEffect(() => {
        setImage(inputImageB64);
        setEditMode(edit);
    }, [inputImageB64, edit]);

    return (
        editMode ?
        <>
            <Row className="justify-content-center">
                <Col>
                    <AvatarEditor className="rounded border"
                        ref={editor}
                        image={image}
                        width={250}
                        height={250}
                        border={25}
                        color={[255, 255, 255, 0.6]}
                        scale={scale}
                        rotate={0}
                    />
                </Col>
            </Row>
            <Row className="justify-content-center">
                <Col>
                    <Form.Range style={{width: 250}} min="1" max="3" step="0.01" value={scale} onChange={(e) => setScale(parseFloat(e.target.value))} />
                </Col>
            </Row>
            <Row className="mb-2">
                <Col>
                    <Button type="primary" onClick={() => {cropImage()}}>Zuschneiden</Button>
                </Col>
            </Row>
            </>
            :
            <Image src={image? image : imgplaceholder} style={{height: 250, width: 250}} className="mb-3" rounded />
    )
}

export default ImageEditor;