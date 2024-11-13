package ricciliao.gateway.pojo;

import org.springframework.http.HttpMethod;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class URIBo implements Serializable {

    @Serial
    private static final long serialVersionUID = -3155439041664787714L;

    public URIBo() {
    }

    public URIBo(String uriPath, String httpMethodStr) {
        this.uriPath = uriPath;
        this.httpMethodStr = httpMethodStr;
        this.httpMethod = HttpMethod.valueOf(httpMethodStr);
    }

    public URIBo(String uriPath, HttpMethod httpMethod) {
        this.uriPath = uriPath;
        this.httpMethod = httpMethod;
        this.httpMethodStr = httpMethod.name();
    }


    private String uriPath;
    private HttpMethod httpMethod;
    private String httpMethodStr;

    public String getUriPath() {
        return uriPath;
    }

    public void setUriPath(String uriPath) {
        this.uriPath = uriPath;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        this.httpMethodStr = httpMethod.name();
    }

    public String getHttpMethodStr() {
        return httpMethodStr;
    }

    public void setHttpMethodStr(String httpMethodStr) {
        this.httpMethodStr = httpMethodStr;
        this.httpMethod = HttpMethod.valueOf(httpMethodStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof URIBo)) return false;
        URIBo uriBo = (URIBo) o;
        return Objects.equals(getUriPath(), uriBo.getUriPath()) && getHttpMethod() == uriBo.getHttpMethod() && Objects.equals(getHttpMethodStr(), uriBo.getHttpMethodStr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUriPath(), getHttpMethod(), getHttpMethodStr());
    }
}
